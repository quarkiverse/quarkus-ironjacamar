package io.quarkiverse.ironjacamar.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Singleton;
import jakarta.resource.spi.XATerminator;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreArrayListManagedConnectionPool;
import org.jboss.jca.core.tx.jbossts.TransactionIntegrationImpl;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.ResourceEndpoint;
import io.quarkiverse.ironjacamar.runtime.CachedConnectionManagerProducer;
import io.quarkiverse.ironjacamar.runtime.ConnectionManagerFactory;
import io.quarkiverse.ironjacamar.runtime.IronJacamarConfig;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkiverse.ironjacamar.runtime.IronJacamarRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.runtime.ArcContainerSupplier;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownListenerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.logging.Log;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.shutdown.ShutdownListener;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.smallrye.common.annotation.Identifier;

class IronJacamarProcessor {

    private static final String FEATURE = "ironjacamar";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void findResourceAdapterKinds(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ResourceAdapterKindBuildItem> kindProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        IndexView index = combinedIndexBuildItem.getIndex();
        var factories = index.getAllKnownImplementors(ResourceAdapterFactory.class);
        if (factories.isEmpty()) {
            Log.warn("No default resource adapter kind found. Please add a dependency to the desired resource adapter");
            //            throw new DeploymentException("No default resource adapter kind found");
        }
        for (ClassInfo factory : factories) {
            AnnotationInstance rak = factory.annotation(ResourceAdapterKind.class);
            if (rak == null) {
                throw new DeploymentException(
                        "Resource adapter factory " + factory + " must be annotated with @ResourceAdapterKind");
            }
            kindProducer.produce(new ResourceAdapterKindBuildItem(rak.value().asString(), factory.name().toString()));
            // Register the factory as an Singleton bean
            additionalBeans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(factory.name().toString())
                    .setDefaultScope(DotNames.SINGLETON)
                    .build());
        }
    }

    @BuildStep
    void registerEndpointsAsApplicationScopedBeans(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        IndexView index = combinedIndexBuildItem.getIndex();
        Set<String> endpoints = index.getAnnotations(ResourceEndpoint.class)
                .stream()
                .map(annotationInstance -> annotationInstance.target().asClass().name().toString())
                .collect(Collectors.toSet());

        // Register message endpoints as @ApplicationScoped beans
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(endpoints)
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .setUnremovable()
                .build());
    }

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(CachedConnectionManagerProducer.class,
                        ConnectionManagerFactory.class,
                        TransactionIntegrationImpl.class)
                .setUnremovable()
                .setDefaultScope(DotNames.SINGLETON)
                .build());
    }

    @BuildStep
    UnremovableBeanBuildItem unremovables() {
        return UnremovableBeanBuildItem.beanTypes(
                ResourceAdapterFactory.class,
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
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void registerSyntheticBeans(
            IronJacamarConfig config,
            IronJacamarRecorder recorder,
            List<ResourceAdapterKindBuildItem> kinds,
            BuildProducer<SyntheticBeanBuildItem> producer) {
        var kindsMap = toMap(kinds);
        for (var entry : config.resourceAdapters().entrySet()) {
            String key = entry.getKey();
            // Using @Identifier to avoid bean name collision
            var raKind = findResourceAdapterKind(entry, kindsMap);
            var ra = entry.getValue().ra();

            // Register the IronJacamarContainer as a Synthetic bean
            producer.produce(SyntheticBeanBuildItem.configure(IronJacamarContainer.class)
                    .scope(Singleton.class)
                    .setRuntimeInit()
                    .unremovable()
                    .addQualifier().annotation(Identifier.class).addValue("value", key).done()
                    .addQualifier().annotation(ResourceAdapterKind.class).addValue("value", raKind.kind).done()
                    .createWith(recorder.createContainerFunction(raKind.kind, ra.config()))
                    .done());
        }
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    ServiceStartBuildItem startResourceAdapters(
            IronJacamarConfig config,
            IronJacamarRecorder recorder,
            CoreVertxBuildItem vertxBuildItem,
            List<ResourceAdapterKindBuildItem> kinds,
            BuildProducer<ShutdownListenerBuildItem> shutdownListenerBuildItems) throws Exception {
        var kindsMap = toMap(kinds);
        // Iterate through all resource adapters configured
        for (var entry : config.resourceAdapters().entrySet()) {
            String key = entry.getKey();
            var raKind = findResourceAdapterKind(entry, kindsMap);
            // Create the resource adapter
            ShutdownListener shutdownListener = recorder.initResourceAdapter(key, raKind.kind,
                    new ArcContainerSupplier(),
                    vertxBuildItem.getVertx());
            if (shutdownListener != null) {
                shutdownListenerBuildItems.produce(new ShutdownListenerBuildItem(shutdownListener));
            }
        }
        return new ServiceStartBuildItem(FEATURE);
    }

    private static Map<String, ResourceAdapterKindBuildItem> toMap(List<ResourceAdapterKindBuildItem> kinds) {
        final Map<String, ResourceAdapterKindBuildItem> kindsMap;
        // Convert list to Map
        if (kinds.size() == 1) {
            kindsMap = Map.of(kinds.get(0).kind, kinds.get(0));
        } else {
            kindsMap = new HashMap<>();
            for (ResourceAdapterKindBuildItem kind : kinds) {
                kindsMap.put(kind.kind, kind);
            }
        }
        return kindsMap;
    }

    private ResourceAdapterKindBuildItem findResourceAdapterKind(
            Map.Entry<String, IronJacamarConfig.ResourceAdapterOuterNamedConfig> entry,
            Map<String, ResourceAdapterKindBuildItem> kinds) {
        Optional<String> kind = entry.getValue().ra().kind();
        final String result;
        if (kind.isPresent()) {
            result = kind.get();
        } else {
            if (kinds.size() == 1) {
                result = kinds.values().iterator().next().kind;
            } else {
                throw new ConfigurationException(
                        "Multiple kinds found, please set the kind config for the " + entry.getKey() + " configuration");
            }
        }
        return kinds.get(result);
    }
}
