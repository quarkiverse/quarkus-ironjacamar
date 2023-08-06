package io.quarkiverse.jca.deployment;

import java.util.List;

import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.XATerminator;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

import io.quarkiverse.jca.runtime.JCAConfig;
import io.quarkiverse.jca.runtime.JCARecorder;
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
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;

class JCAProcessor {

    private static final String FEATURE = "jca";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    //    void indexResourceAdapters(JCAConfig config, BuildProducer<IndexDependencyBuildItem> producer) {
    //        producer.produce(new IndexDependencyBuildItem());
    //    }

    @BuildStep
    void findResourceAdapters(JCAConfig config,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ResourceAdapterBuildItem> resourceAdapterBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        IndexView index = combinedIndexBuildItem.getIndex();
        for (ClassInfo implementor : index.getAllKnownImplementors(ResourceAdapter.class)) {
            String resourceAdapterClassName = implementor.name().toString();
            resourceAdapterBuildItemBuildProducer.produce(new ResourceAdapterBuildItem(resourceAdapterClassName));
            // Register ResourceAdapter as @Singleton beans
            additionalBeans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClass(resourceAdapterClassName)
                    .setDefaultScope(DotNames.SINGLETON)
                    .setUnremovable()
                    .build());
        }
        // Register ManagedConnectionFactory as @Singleton beans
        //        for (ClassInfo implementor : index.getAllKnownImplementors(ManagedConnectionFactory.class)) {
        //            System.out.println("ClassInfo: " + implementor.name());
        //            beansProducer.produce(SyntheticBeanBuildItem.configure(implementor.name())
        //                    .defaultBean()
        //                    .scope(Singleton.class)
        //                    .done());
        //
        //        }

        //        additionalBeans.produce(new AdditionalBeanBuildItem(ObjectMapperProducer.class));

    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.beanTypes(TransactionSynchronizationRegistry.class, XATerminator.class);
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem startResourceAdapters(
            JCAConfig config,
            List<ResourceAdapterBuildItem> resourceAdapterBuildItems,
            JCARecorder recorder,
            CoreVertxBuildItem vertxBuildItem) {
        for (ResourceAdapterBuildItem resourceAdapterBuildItem : resourceAdapterBuildItems) {
            recorder.deployResourceAdapter(vertxBuildItem.getVertx(), resourceAdapterBuildItem.className,
                    config.resourceAdapterConfigs.get("artemis").configProperties);
        }
        return new ServiceStartBuildItem(FEATURE);
    }
}
