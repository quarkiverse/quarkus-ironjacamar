package io.quarkiverse.ironjacamar.deployment;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.XATerminator;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreArrayListManagedConnectionPool;
import org.jboss.jca.core.tx.jbossts.TransactionIntegrationImpl;

import io.quarkiverse.ironjacamar.ResourceAdapterSupport;
import io.quarkiverse.ironjacamar.ResourceEndpoint;
import io.quarkiverse.ironjacamar.runtime.JCARecorder;
import io.quarkiverse.ironjacamar.runtime.connectionmanager.ConnectionManagerProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownListenerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.runtime.shutdown.ShutdownListener;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;

class JCAProcessor {

    private static final String FEATURE = "ironjacamar";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void findResourceAdapters(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ResourceAdapterBuildItem> resourceAdapterBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        IndexView index = combinedIndexBuildItem.getIndex();
        // TODO: Check if endpoint is supported by the resource adapter
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

        for (ClassInfo implementor : index.getAllKnownImplementors(ResourceAdapter.class)) {
            String resourceAdapterClassName = implementor.name().toString();
            ResourceAdapterBuildItem resourceAdapterBuildItem = ResourceAdapterBuildItem
                    .builder(resourceAdapterClassName)
                    .addEndpoints(endpoints)
                    .build();
            resourceAdapterBuildItemBuildProducer.produce(resourceAdapterBuildItem);
            // Register ResourceAdapter as @Singleton beans
            additionalBeans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClass(resourceAdapterClassName)
                    .setDefaultScope(DotNames.SINGLETON)
                    .setUnremovable()
                    .build());
        }
        // Register CDI managed beans
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(ConnectionManagerProducer.class, TransactionIntegrationImpl.class)
                .setDefaultScope(DotNames.SINGLETON)
                .build());
    }

    @BuildStep
    UnremovableBeanBuildItem unremovables() {
        return UnremovableBeanBuildItem.beanTypes(
                ResourceAdapterSupport.class,
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
    ServiceStartBuildItem startResourceAdapters(
            List<ResourceAdapterBuildItem> resourceAdapterBuildItems,
            JCARecorder recorder,
            CoreVertxBuildItem vertxBuildItem,
            BuildProducer<ShutdownListenerBuildItem> shutdownListenerBuildItems) throws Exception {
        for (ResourceAdapterBuildItem resourceAdapterBuildItem : resourceAdapterBuildItems) {
            ShutdownListener shutdownListener = recorder.initResourceAdapter(
                    vertxBuildItem.getVertx(),
                    resourceAdapterBuildItem.resourceAdapterClassName,
                    resourceAdapterBuildItem.endpointClassnames);
            if (shutdownListener != null) {
                shutdownListenerBuildItems.produce(new ShutdownListenerBuildItem(shutdownListener));
            }
        }
        return new ServiceStartBuildItem(FEATURE);
    }
}
