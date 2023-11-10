package io.quarkiverse.ironjacamar.deployment;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.resource.spi.XATerminator;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreArrayListManagedConnectionPool;
import org.jboss.jca.core.recovery.DefaultRecoveryPlugin;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.tx.jbossts.TransactionIntegrationImpl;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.ResourceAdapterTypes;
import io.quarkiverse.ironjacamar.ResourceEndpoint;
import io.quarkiverse.ironjacamar.runtime.ConnectionManagerFactory;
import io.quarkiverse.ironjacamar.runtime.IronJacamarBuildtimeConfig;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkiverse.ironjacamar.runtime.IronJacamarRecorder;
import io.quarkiverse.ironjacamar.runtime.IronJacamarSupport;
import io.quarkiverse.ironjacamar.runtime.QuarkusIronJacamarLogger;
import io.quarkiverse.ironjacamar.runtime.security.QuarkusSecurityIntegration;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Future;

class IronJacamarProcessor {

    private static final String FEATURE = "ironjacamar";
    private static final AnnotationInstance DEFAULT_QUALIFIER = AnnotationInstance.builder(DotNames.DEFAULT).build();

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(TransactionIntegrationImpl.class, DefaultRecoveryPlugin.class)
                .setUnremovable()
                .setDefaultScope(DotNames.SINGLETON)
                .build());
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                        ConnectionManagerFactory.class,
                        IronJacamarSupport.class)
                .build());
    }

    @BuildStep
    UnremovableBeanBuildItem unremovables() {
        return UnremovableBeanBuildItem.beanTypes(
                TransactionSynchronizationRegistry.class,
                XATerminator.class);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return ReflectiveClassBuildItem.builder(
                SemaphoreArrayListManagedConnectionPool.class)
                .build();
    }

    @BuildStep
    NativeImageResourceBuildItem registerNativeResources() {
        return new NativeImageResourceBuildItem("poolstatistics.properties");
    }

    @BuildStep
    void findResourceAdapterKinds(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ResourceAdapterKindBuildItem> kindProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        IndexView index = combinedIndexBuildItem.getIndex();

        var factories = index.getAllKnownImplementors(ResourceAdapterFactory.class);
        if (factories.isEmpty()) {
            QuarkusIronJacamarLogger.log.noDefaultResourceAdapterKindFound();
            return;
        }
        for (ClassInfo factory : factories) {
            AnnotationInstance rak = factory.annotation(ResourceAdapterKind.class);
            if (rak == null) {
                throw QuarkusIronJacamarLogger.log.resourceAdapterFactoryMustBeAnnotatedException(factory.name().toString());
            }

            kindProducer.produce(new ResourceAdapterKindBuildItem(rak.value().asString(), factory.name().toString()));

            // Register the factory as a managed bean
            additionalBeans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(factory.name().toString())
                    .build());
        }
    }

    @BuildStep
    void registerResourceEndpoints(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        IndexView index = combinedIndexBuildItem.getIndex();
        String[] endpoints = index.getAnnotations(ResourceEndpoint.class)
                .stream()
                .map(annotationInstance -> annotationInstance.target().asClass().name().toString())
                .toArray(String[]::new);

        // Register message endpoints as @ApplicationScoped beans
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(endpoints)
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .setUnremovable()
                .build());

        // Add reflective access to message endpoints (needed for io.quarkiverse.ironjacamar.runtime.endpoint.DefaultMessageEndpointFactory#isDeliveryTransacted)
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(endpoints).methods().build());
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT)
    void registerSingletonSyntheticBeans(BuildProducer<SyntheticBeanBuildItem> producer,
            IronJacamarRecorder recorder) {
        // Register the CachedConnectionManager as a Singleton bean
        producer.produce(SyntheticBeanBuildItem.configure(CachedConnectionManager.class)
                .scope(BuiltinScope.SINGLETON.getInfo())
                .setRuntimeInit()
                .unremovable()
                .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionIntegration.class)))
                .createWith(recorder.createCachedConnectionManager())
                .destroyer(mc -> {
                    // Invoke CachedConnectionManager#stop()
                    mc.invokeInterfaceMethod(MethodDescriptor.ofMethod(CachedConnectionManager.class, "stop", void.class),
                            mc.getMethodParam(0));
                    // return is void
                    mc.returnValue(null);
                })
                .done());

        producer.produce(SyntheticBeanBuildItem.configure(QuarkusSecurityIntegration.class)
                .scope(BuiltinScope.DEPENDENT.getInfo())
                .unremovable()
                .createWith(recorder.createSecurityIntegration())
                .done());
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT)
    void registerSyntheticBeans(
            IronJacamarBuildtimeConfig config,
            IronJacamarRecorder recorder,
            CombinedIndexBuildItem combinedIndexBuildItem,
            List<ResourceAdapterKindBuildItem> kinds,
            BuildProducer<SyntheticBeanBuildItem> producer,
            BuildProducer<ContainerCreatedBuildItem> createdProducer) {
        IndexView index = combinedIndexBuildItem.getIndex();
        Type containerType = Type.create(DotName.createSimple(IronJacamarContainer.class), Type.Kind.CLASS);
        var kindsMap = kinds.stream().collect(Collectors.toMap(ResourceAdapterKindBuildItem::getKind, Function.identity()));
        boolean single = kindsMap.size() == 1;
        for (var entry : config.resourceAdapters().entrySet()) {
            String key = entry.getKey();
            // Using @Identifier to avoid bean name collision
            var raKind = resolveKind(entry, kindsMap);
            if (raKind == null) {
                continue;
            }
            ClassInfo raf = index.getClassByName(raKind.resourceAdapterFactoryClassName);
            Type[] connectionFactoryProvides = raf.annotation(ResourceAdapterTypes.class).value("connectionFactoryTypes")
                    .asClassArray();

            AnnotationInstance qualifier = AnnotationInstance.builder(Identifier.class).add("value", key).build();

            // Register the IronJacamarContainer as a Synthetic bean
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(IronJacamarContainer.class)
                    .scope(BuiltinScope.SINGLETON.getInfo())
                    .setRuntimeInit()
                    .addQualifier(qualifier)
                    .unremovable()
                    .createWith(recorder.createContainerFunction(key, raKind.kind))
                    .addInjectionPoint(ClassType.create(DotName.createSimple(IronJacamarSupport.class)))
                    .destroyer(BeanDestroyer.CloseableDestroyer.class);
            // Don't need to specify the identifier if a single Resource Adapter is deployed
            if (single) {
                configurator.addQualifier(DEFAULT_QUALIFIER);
            }
            producer.produce(configurator.done());

            // Connection Factory bean
            SyntheticBeanBuildItem.ExtendedBeanConfigurator cfConfigurator = SyntheticBeanBuildItem.configure(Object.class)
                    .scope(BuiltinScope.SINGLETON.getInfo())
                    .setRuntimeInit()
                    .addQualifier(qualifier)
                    .types(connectionFactoryProvides)
                    .addInjectionPoint(containerType, qualifier)
                    .unremovable()
                    .createWith(recorder.createConnectionFactory(key));
            if (single) {
                cfConfigurator.addQualifier(DEFAULT_QUALIFIER);
            }
            producer.produce(cfConfigurator.done());
            createdProducer.produce(new ContainerCreatedBuildItem(key));
        }
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT)
    void initDefaultBootstrapContext(BeanContainerBuildItem beanContainerBuildItem, IronJacamarRecorder recorder) {
        // Create the default bootstrap context
        recorder.initDefaultBootstrapContext(beanContainerBuildItem.getValue());
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    void startResourceAdapters(
            List<ContainerCreatedBuildItem> containers,
            IronJacamarRecorder recorder,
            CoreVertxBuildItem vertxBuildItem,
            BeanContainerBuildItem beanContainerBuildItem,
            BuildProducer<ContainerStartedBuildItem> startedProducer) {
        // Iterate through all resource adapters configured
        for (ContainerCreatedBuildItem container : containers) {
            // Start the resource adapter
            RuntimeValue<Future<String>> futureRuntimeValue = recorder.initResourceAdapter(beanContainerBuildItem.getValue(),
                    container.identifier,
                    vertxBuildItem.getVertx());
            startedProducer.produce(new ContainerStartedBuildItem(container.identifier, futureRuntimeValue));
        }
    }

    /**
     * Find all classes with @ResourceEndpoint and activate them for the respective {@link IronJacamarContainer}
     */
    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    @Produce(IronJacamarInitBuildItem.class)
    ServiceStartBuildItem activateEndpoints(CombinedIndexBuildItem combinedIndexBuildItem,
            IronJacamarRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem,
            List<ContainerStartedBuildItem> containers) {
        IndexView index = combinedIndexBuildItem.getIndex();
        boolean single = containers.size() == 1;
        if (single) {
            ContainerStartedBuildItem container = containers.get(0);
            Collection<AnnotationInstance> annotations = index.getAnnotations(ResourceEndpoint.class);
            for (AnnotationInstance instance : annotations) {
                ClassInfo classInfo = instance.target().asClass();
                String activationSpecId = getActivationSpecId(instance);
                String resourceEndpoint = classInfo.name().toString();
                // TODO: Extract config
                Map<String, String> buildTimeConfig = Map.of();
                recorder.activateEndpoint(beanContainerBuildItem.getValue(), container.futureRuntimeValue, container.identifier,
                        activationSpecId,
                        resourceEndpoint,
                        buildTimeConfig);
            }
        } else {
            // More than one ResourceAdapter found. Activate the endpoints for the respective container
            for (ContainerStartedBuildItem container : containers) {
                // Filter out the endpoints for the respective container
                Collection<AnnotationInstance> annotations = index.getAnnotations(ResourceEndpoint.class);
                for (AnnotationInstance instance : annotations) {
                    AnnotationInstance annotation = instance.target().declaredAnnotation(Identifier.class);
                    if (annotation == null) {
                        throw QuarkusIronJacamarLogger.log.useIdentifierAnnotation(instance.target().toString());
                    }
                    // Test if the identifier matches the container
                    if (container.identifier.equals(annotation.value().asString())) {
                        String activationSpecId = getActivationSpecId(instance);
                        String resourceEndpoint = instance.target().asClass().name().toString();
                        // TODO: Extract config
                        Map<String, String> buildTimeConfig = Map.of();
                        recorder.activateEndpoint(beanContainerBuildItem.getValue(), container.futureRuntimeValue,
                                container.identifier, activationSpecId,
                                resourceEndpoint,
                                buildTimeConfig);
                    }
                }
            }
        }
        return new ServiceStartBuildItem(FEATURE);
    }

    private static String getActivationSpecId(AnnotationInstance instance) {
        AnnotationValue activationSpec = instance.value("activationSpecConfigKey");
        if (activationSpec != null) {
            return activationSpec.asString();
        }
        return null;
    }

    private static ResourceAdapterKindBuildItem resolveKind(
            Map.Entry<String, IronJacamarBuildtimeConfig.ResourceAdapterOuterNamedConfig> entry,
            Map<String, ResourceAdapterKindBuildItem> kinds) {
        Optional<String> kind = entry.getValue().ra().kind();
        final String result;
        if (kind.isPresent()) {
            result = kind.get();
        } else {
            if (kinds.isEmpty()) {
                return null;
            } else if (kinds.size() == 1) {
                result = kinds.values().iterator().next().kind;
            } else {
                throw QuarkusIronJacamarLogger.log.multipleKindsFound(kinds.keySet(), entry.getKey());
            }
        }
        return kinds.get(result);
    }
}
