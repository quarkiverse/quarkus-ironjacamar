package io.quarkiverse.ironjacamar.deployment;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.jboss.jca.core.spi.recovery.RecoveryPlugin;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.tx.jbossts.TransactionIntegrationImpl;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.ResourceAdapterTypes;
import io.quarkiverse.ironjacamar.ResourceEndpoint;
import io.quarkiverse.ironjacamar.runtime.CachedConnectionManagerBeanCreator;
import io.quarkiverse.ironjacamar.runtime.ConnectionFactoryBeanCreator;
import io.quarkiverse.ironjacamar.runtime.ConnectionManagerFactory;
import io.quarkiverse.ironjacamar.runtime.IronJacamarBuildtimeConfig;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainerBeanCreator;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainerStart;
import io.quarkiverse.ironjacamar.runtime.IronJacamarRecorder;
import io.quarkiverse.ironjacamar.runtime.IronJacamarRuntimeConfig;
import io.quarkiverse.ironjacamar.runtime.IronJacamarSupport;
import io.quarkiverse.ironjacamar.runtime.QuarkusIronJacamarLogger;
import io.quarkiverse.ironjacamar.runtime.TransactionRecoveryManager;
import io.quarkiverse.ironjacamar.runtime.TransactionRecoveryManagerBeanCreator;
import io.quarkiverse.ironjacamar.runtime.listener.ResourceAdapterLifecycleListener;
import io.quarkiverse.ironjacamar.runtime.security.QuarkusSecurityIntegration;
import io.quarkiverse.ironjacamar.runtime.security.QuarkusSecurityIntegrationBeanCreator;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;

class IronJacamarProcessor {

    private static final String FEATURE = "ironjacamar";

    private static final String BOOTSTRAP_CONTEXT_SERVICE_NAME = "io.quarkiverse.ironjacamar.bootstrap-context";
    private static final String CONTAINER_START_SERVICE_PREFIX = "io.quarkiverse.ironjacamar.container-start.";
    private static final String ENDPOINT_ACTIVATION_SERVICE_NAME = "io.quarkiverse.ironjacamar.endpoint-activation";
    private static final String IDLE_REMOVER_SERVICE_NAME = "io.quarkiverse.ironjacamar.idle-remover";
    private static final String CONNECTION_VALIDATOR_SERVICE_NAME = "io.quarkiverse.ironjacamar.connection-validator";

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

        var factories = index.getAllKnownImplementations(ResourceAdapterFactory.class);
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
            List<ResourceEndpointBuildItem> resourceEndpoints,
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

        // Make listeners unremovable
        List<String> listeners = index.getAllKnownImplementations(ResourceAdapterLifecycleListener.class)
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .toList();

        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(listeners)
                .setUnremovable()
                .build());

        // Add reflective access to message endpoints (needed for io.quarkiverse.ironjacamar.runtime.endpoint.DefaultMessageEndpointFactory#isDeliveryTransacted)
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(endpoints).methods().build());

        // Endpoints contributed by other extensions are beans already, they just need the reflective access
        if (!resourceEndpoints.isEmpty()) {
            String[] contributedEndpoints = resourceEndpoints.stream()
                    .map(ResourceEndpointBuildItem::endpointClassName)
                    .toArray(String[]::new);
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(contributedEndpoints).methods().build());
        }
    }

    @BuildStep
    void registerSingletonSyntheticBeans(BuildProducer<SyntheticBeanBuildItem> producer) {
        // Register the CachedConnectionManager as a Singleton bean
        producer.produce(SyntheticBeanBuildItem.configure(CachedConnectionManager.class)
                .scope(BuiltinScope.SINGLETON.getInfo())
                .setRuntimeInit()
                .unremovable()
                .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionIntegration.class)))
                .creator(CachedConnectionManagerBeanCreator.class)
                .destroyer(mc -> {
                    // Invoke CachedConnectionManager#stop()
                    BlockCreator bc = mc.destroyMethod();
                    bc.invokeInterface(MethodDesc.of(CachedConnectionManager.class, "stop", void.class),
                            mc.destroyedInstance());
                    // return is void
                    bc.return_();
                })
                .done());

        // Produce TransactionRecoveryManager
        producer.produce(SyntheticBeanBuildItem.configure(TransactionRecoveryManager.class)
                .scope(BuiltinScope.SINGLETON.getInfo())
                .setRuntimeInit()
                .unremovable()
                .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionIntegration.class)))
                .addInjectionPoint(ClassType.create(DotName.createSimple(RecoveryPlugin.class)))
                .creator(TransactionRecoveryManagerBeanCreator.class)
                .autoClose(true)
                .done());
    }

    @BuildStep
    void registerSyntheticBeans(
            IronJacamarBuildtimeConfig config,
            CombinedIndexBuildItem combinedIndexBuildItem,
            List<ResourceAdapterKindBuildItem> kinds,
            BuildProducer<SyntheticBeanBuildItem> producer,
            BuildProducer<ContainerCreatedBuildItem> createdProducer) {
        // Register security integration as a dependent bean
        producer.produce(SyntheticBeanBuildItem.configure(QuarkusSecurityIntegration.class)
                .scope(BuiltinScope.DEPENDENT.getInfo())
                .unremovable()
                .creator(QuarkusSecurityIntegrationBeanCreator.class)
                .done());
        IndexView index = combinedIndexBuildItem.getIndex();
        Type containerType = Type.create(DotName.createSimple(IronJacamarContainer.class), Type.Kind.CLASS);
        var kindsMap = kinds.stream().collect(Collectors.toMap(ResourceAdapterKindBuildItem::kind, Function.identity()));
        boolean single = kindsMap.size() == 1;
        for (var entry : config.resourceAdapters().entrySet()) {
            String key = entry.getKey();
            // Using @Identifier to avoid bean name collision
            var raKind = resolveKind(entry, kindsMap);
            if (raKind == null) {
                continue;
            }
            AnnotationInstance qualifier = AnnotationInstance.builder(Identifier.class).add("value", key).build();

            // Register the IronJacamarContainer as a Synthetic bean
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(IronJacamarContainer.class)
                    .scope(BuiltinScope.SINGLETON.getInfo())
                    .setRuntimeInit()
                    .addQualifier(qualifier)
                    .unremovable()
                    .creator(IronJacamarContainerBeanCreator.class)
                    .param("id", key)
                    .param("kind", raKind.kind())
                    .addInjectionPoint(ClassType.create(DotName.createSimple(Vertx.class)))
                    .addInjectionPoint(ClassType.create(DotName.createSimple(IronJacamarSupport.class)))
                    .autoClose(true);
            // Don't need to specify the identifier if a single Resource Adapter is deployed
            if (single) {
                configurator.addQualifier(DotNames.DEFAULT);
            }
            producer.produce(configurator.done());

            ClassInfo raf = index.getClassByName(raKind.resourceAdapterFactoryClassName());
            if (raf.hasAnnotation(ResourceAdapterTypes.class)) {
                Type[] connectionFactoryProvides = raf.annotation(ResourceAdapterTypes.class).value("connectionFactoryTypes")
                        .asClassArray();
                for (Type type : connectionFactoryProvides) {
                    // Register the connection factory as a Synthetic bean
                    SyntheticBeanBuildItem.ExtendedBeanConfigurator cfConfigurator = SyntheticBeanBuildItem
                            .configure(type.name())
                            .scope(BuiltinScope.DEPENDENT.getInfo())
                            .setRuntimeInit()
                            .addQualifier(qualifier)
                            .types(type)
                            .addInjectionPoint(containerType, qualifier)
                            .unremovable()
                            .creator(ConnectionFactoryBeanCreator.class)
                            .param("id", key);
                    if (single) {
                        cfConfigurator.addQualifier(DotNames.DEFAULT);
                    }
                    producer.produce(cfConfigurator.done());
                }
            } else {
                QuarkusIronJacamarLogger.log.resourceAdapterTypesNotDefined(raf.name().toString());
            }
            createdProducer.produce(new ContainerCreatedBuildItem(key, raKind.kind()));
        }
    }

    @BuildStep
    void initDefaultBootstrapContext(ServiceRegistrar serviceRegistrar) {
        // Create the default bootstrap context during static initialization
        serviceRegistrar
                .forService(BOOTSTRAP_CONTEXT_SERVICE_NAME)
                .atPhase(Phase.STATIC_INIT)
                .require(BeanContainer.class)
                .onStart((ctx, beanContainer) -> IronJacamarRecorder.initDefaultBootstrapContext(beanContainer));
    }

    @BuildStep
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    // afterBuildItem() is a transitional API, required until the synthetic beans accessed by the container start
    // service are initialized by the service graph instead of a bytecode recorder
    @SuppressWarnings("deprecation")
    void startResourceAdapters(
            List<ContainerCreatedBuildItem> containers,
            ServiceRegistrar serviceRegistrar,
            BuildProducer<ContainerStartedBuildItem> startedProducer) {
        // Iterate through all resource adapters configured
        for (ContainerCreatedBuildItem container : containers) {
            String key = container.identifier();
            String startServiceName = CONTAINER_START_SERVICE_PREFIX + key;
            // Start the resource adapter
            serviceRegistrar
                    .forService(IronJacamarContainerStart.class, startServiceName)
                    .require(BeanContainer.class)
                    .require(Vertx.class)
                    .require(IronJacamarRuntimeConfig.class)
                    .afterBuildItem(SyntheticBeansRuntimeInitBuildItem.class)
                    .onStart((ctx, beanContainer, vertx, runtimeConfig) -> IronJacamarRecorder
                            .initResourceAdapter(beanContainer, key, vertx, runtimeConfig));
            startedProducer.produce(new ContainerStartedBuildItem(key, startServiceName));
        }
    }

    /**
     * Find all classes with @ResourceEndpoint, as well as the endpoints contributed through
     * {@link ResourceEndpointBuildItem}, and activate them for the respective {@link IronJacamarContainer}.
     * <p>
     * Endpoints listed in a {@link ResourceEndpointIgnoredBuildItem} are not activated.
     */
    @BuildStep
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    @Produce(IronJacamarInitBuildItem.class)
    ServiceStartBuildItem activateEndpoints(CombinedIndexBuildItem combinedIndexBuildItem,
            ServiceRegistrar serviceRegistrar,
            List<ContainerStartedBuildItem> containers,
            List<ResourceEndpointBuildItem> resourceEndpoints,
            List<ResourceEndpointIgnoredBuildItem> ignoredResourceEndpoints) {
        if (containers.isEmpty()) {
            // No resource adapter configured, there is nothing to activate
            return new ServiceStartBuildItem(FEATURE);
        }
        IndexView index = combinedIndexBuildItem.getIndex();
        // Contributed endpoints may be ignored too, as long as their class is available in the index
        List<ResourceEndpointBuildItem> endpoints = resourceEndpoints.stream()
                .filter(endpoint -> !isIgnored(index.getClassByName(endpoint.endpointClassName()),
                        ignoredResourceEndpoints))
                .toList();
        boolean single = containers.size() == 1;
        validateContainerIds(endpoints, containers, single);
        if (single) {
            ContainerStartedBuildItem container = containers.get(0);
            Collection<AnnotationInstance> annotations = index.getAnnotations(ResourceEndpoint.class);
            for (AnnotationInstance instance : annotations) {
                ClassInfo classInfo = instance.target().asClass();
                if (isIgnored(classInfo, ignoredResourceEndpoints)) {
                    continue;
                }
                String activationSpecId = getActivationSpecId(instance);
                String resourceEndpoint = classInfo.name().toString();
                activateEndpoint(serviceRegistrar, container, activationSpecId, resourceEndpoint);
            }
            for (ResourceEndpointBuildItem endpoint : endpoints) {
                // The container id is optional when a single resource adapter is deployed
                activateEndpoint(serviceRegistrar, container, endpoint.activationSpecConfigKey(),
                        endpoint.endpointClassName());
            }
        } else {
            // Activate the endpoints for the respective container
            for (ContainerStartedBuildItem container : containers) {
                // Filter out the endpoints for the respective container
                Collection<AnnotationInstance> annotations = index.getAnnotations(ResourceEndpoint.class);
                for (AnnotationInstance instance : annotations) {
                    if (isIgnored(instance.target().asClass(), ignoredResourceEndpoints)) {
                        continue;
                    }
                    AnnotationInstance annotation = instance.target().declaredAnnotation(Identifier.class);
                    if (annotation == null) {
                        throw QuarkusIronJacamarLogger.log.useIdentifierAnnotation(instance.target().toString());
                    }
                    // Test if the identifier matches the container
                    if (container.identifier().equals(annotation.value().asString())) {
                        String activationSpecId = getActivationSpecId(instance);
                        String resourceEndpoint = instance.target().asClass().name().toString();
                        activateEndpoint(serviceRegistrar, container, activationSpecId, resourceEndpoint);
                    }
                }
                for (ResourceEndpointBuildItem endpoint : endpoints) {
                    if (container.identifier().equals(endpoint.containerId())) {
                        activateEndpoint(serviceRegistrar, container, endpoint.activationSpecConfigKey(),
                                endpoint.endpointClassName());
                    }
                }
            }
        }
        return new ServiceStartBuildItem(FEATURE);
    }

    @BuildStep
    @Consume(IronJacamarInitBuildItem.class)
    // afterBuildItem() is a transitional API, required until the synthetic beans accessed by the pool services
    // (such as ManagedExecutor) are initialized by the service graph instead of a bytecode recorder
    @SuppressWarnings("deprecation")
    void startPoolServices(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar
                .forService(IDLE_REMOVER_SERVICE_NAME)
                .require(BeanContainer.class)
                .require(IronJacamarRuntimeConfig.class)
                .afterBuildItem(SyntheticBeansRuntimeInitBuildItem.class)
                .onStart((ctx, beanContainer, runtimeConfig) -> IronJacamarRecorder.startIdleRemover(ctx, beanContainer,
                        runtimeConfig));
        serviceRegistrar
                .forService(CONNECTION_VALIDATOR_SERVICE_NAME)
                .require(BeanContainer.class)
                .require(IronJacamarRuntimeConfig.class)
                .afterBuildItem(SyntheticBeansRuntimeInitBuildItem.class)
                .onStart((ctx, beanContainer, runtimeConfig) -> IronJacamarRecorder.startConnectionValidator(ctx,
                        beanContainer, runtimeConfig));
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    public CardPageBuildItem devUI() {
        CardPageBuildItem card = new CardPageBuildItem();
        card.addLibraryVersion("org.jboss.ironjacamar", "ironjacamar-core-impl", "IronJacamar",
                "https://www.ironjacamar.org/");
        card.addLibraryVersion("jakarta.resource", "jakarta.resource-api", "Jakarta Connectors",
                "https://jakarta.ee/specifications/connectors/");
        return card;
    }

    /**
     * Register a service that activates the given endpoint once the respective resource adapter is started.
     */
    private static void activateEndpoint(ServiceRegistrar serviceRegistrar, ContainerStartedBuildItem container,
            String activationSpecConfigId, String endpointClassName) {
        String resourceAdapterId = container.identifier();
        serviceRegistrar
                .forService(List.of(ENDPOINT_ACTIVATION_SERVICE_NAME, resourceAdapterId, endpointClassName))
                .require(BeanContainer.class)
                .require(IronJacamarContainerStart.class, container.startServiceName())
                .onStart((ctx, beanContainer, containerStart) -> IronJacamarRecorder.activateEndpoint(beanContainer,
                        containerStart, resourceAdapterId, activationSpecConfigId, endpointClassName,
                        // TODO: Extract config
                        Map.of()));
    }

    /**
     * Validate the container ids of the contributed endpoints: they are required as soon as more than one resource
     * adapter is deployed, and they must always match a deployed one.
     */
    private static void validateContainerIds(List<ResourceEndpointBuildItem> endpoints,
            List<ContainerStartedBuildItem> containers, boolean single) {
        if (endpoints.isEmpty()) {
            return;
        }
        Set<String> containerIds = containers.stream()
                .map(ContainerStartedBuildItem::identifier)
                .collect(Collectors.toSet());
        for (ResourceEndpointBuildItem endpoint : endpoints) {
            String containerId = endpoint.containerId();
            if (containerId == null) {
                if (!single) {
                    throw QuarkusIronJacamarLogger.log.useContainerId(endpoint.endpointClassName());
                }
            } else if (!containerIds.contains(containerId)) {
                throw QuarkusIronJacamarLogger.log.unknownContainerId(endpoint.endpointClassName(), containerId);
            }
        }
    }

    private static boolean isIgnored(ClassInfo endpointClass, List<ResourceEndpointIgnoredBuildItem> ignoredResourceEndpoints) {
        if (endpointClass == null) {
            // Endpoints generated at build time are not in the index, there is nothing to match against
            return false;
        }
        for (ResourceEndpointIgnoredBuildItem ignored : ignoredResourceEndpoints) {
            if (ignored.test(endpointClass)) {
                return true;
            }
        }
        return false;
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
                result = kinds.values().iterator().next().kind();
            } else {
                throw QuarkusIronJacamarLogger.log.multipleKindsFound(kinds.keySet(), entry.getKey());
            }
        }
        return kinds.get(result);
    }
}
