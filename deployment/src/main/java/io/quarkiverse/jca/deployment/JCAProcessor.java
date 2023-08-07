package io.quarkiverse.jca.deployment;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.XATerminator;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

import io.quarkiverse.jca.runtime.JCAConfig;
import io.quarkiverse.jca.runtime.JCARecorder;
import io.quarkiverse.jca.runtime.api.ResourceEndpoint;
import io.quarkiverse.jca.runtime.spi.ResourceAdapterSupport;
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
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.shutdown.ShutdownListener;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;

class JCAProcessor {

    private static final String FEATURE = "jca";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void findResourceAdapters(JCAConfig config,
            CombinedIndexBuildItem combinedIndexBuildItem,
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
            resourceAdapterBuildItemBuildProducer
                    .produce(new ResourceAdapterBuildItem(resourceAdapterClassName, endpoints));
            // Register ResourceAdapter as @Singleton beans
            additionalBeans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClass(resourceAdapterClassName)
                    .setDefaultScope(DotNames.SINGLETON)
                    .setUnremovable()
                    .build());
        }
    }

    @BuildStep
    UnremovableBeanBuildItem unremovables() {
        return UnremovableBeanBuildItem.beanTypes(
                ResourceAdapterSupport.class,
                TransactionSynchronizationRegistry.class,
                XATerminator.class);
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem startResourceAdapters(
            JCAConfig config,
            List<ResourceAdapterBuildItem> resourceAdapterBuildItems,
            JCARecorder recorder,
            CoreVertxBuildItem vertxBuildItem,
            BuildProducer<ShutdownListenerBuildItem> shutdownListenerBuildItems) {
        for (ResourceAdapterBuildItem resourceAdapterBuildItem : resourceAdapterBuildItems) {
            RuntimeValue<ResourceAdapter> resourceAdapter = recorder.deployResourceAdapter(vertxBuildItem.getVertx(),
                    resourceAdapterBuildItem.className);
            ShutdownListener shutdownListener = recorder.activateEndpoints(resourceAdapter,
                    resourceAdapterBuildItem.endpointsClassNames);
            shutdownListenerBuildItems.produce(new ShutdownListenerBuildItem(shutdownListener));
        }
        return new ServiceStartBuildItem(FEATURE);
    }
}
