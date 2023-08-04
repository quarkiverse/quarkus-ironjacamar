package io.quarkiverse.jca.deployment;

import java.util.Collections;
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
            BuildProducer<AdditionalBeanBuildItem> beansProducer) {
        IndexView index = combinedIndexBuildItem.getIndex();
        for (ClassInfo implementor : index.getAllKnownImplementors(ResourceAdapter.class)) {
            resourceAdapterBuildItemBuildProducer.produce(new ResourceAdapterBuildItem(implementor));
        }
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.beanTypes(TransactionSynchronizationRegistry.class, XATerminator.class);
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem startResourceAdapters(
            List<ResourceAdapterBuildItem> resourceAdapterBuildItems,
            JCARecorder recorder,
            CoreVertxBuildItem vertxBuildItem) {
        for (ResourceAdapterBuildItem resourceAdapterBuildItem : resourceAdapterBuildItems) {
            ClassInfo classInfo = resourceAdapterBuildItem.classInfo;
            recorder.deployResourceAdapter(vertxBuildItem.getVertx(), classInfo.name().toString(), Collections.emptyMap());
        }
        return new ServiceStartBuildItem(FEATURE);
    }
}
