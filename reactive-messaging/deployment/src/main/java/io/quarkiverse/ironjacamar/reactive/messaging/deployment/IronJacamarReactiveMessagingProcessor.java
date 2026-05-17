package io.quarkiverse.ironjacamar.reactive.messaging.deployment;

import io.quarkiverse.ironjacamar.reactive.messaging.runtime.IncomingResourceAdapterSupport;
import io.quarkiverse.ironjacamar.reactive.messaging.runtime.OutgoingResourceAdapterSupport;
import io.quarkiverse.ironjacamar.reactive.messaging.runtime.impl.IronJacamarConnector;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class IronJacamarReactiveMessagingProcessor {

    private static final String FEATURE = "ironjacamar-reactive-messaging";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerConnector() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(IronJacamarConnector.class)
                .setUnremovable()
                .build();
    }

    @BuildStep
    UnremovableBeanBuildItem markSpiImplementationsUnremovable() {
        return UnremovableBeanBuildItem.beanTypes(IncomingResourceAdapterSupport.class,
                OutgoingResourceAdapterSupport.class);
    }
}
