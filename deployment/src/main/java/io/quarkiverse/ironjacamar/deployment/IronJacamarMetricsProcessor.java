package io.quarkiverse.ironjacamar.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.ironjacamar.runtime.IronJacamarBuildtimeConfig;
import io.quarkiverse.ironjacamar.runtime.metrics.IronJacamarPoolMetricsConsumer;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;

/**
 * Processor that registers metrics for IronJacamar
 */
public class IronJacamarMetricsProcessor {

    /**
     * Constructor
     */
    public IronJacamarMetricsProcessor() {

    }

    /**
     * Register metrics
     *
     * @param metrics the metrics
     * @param containers the containers
     * @param buildtimeConfig the buildtime config
     * @param metricsCapability the metrics capability
     */
    @BuildStep
    void registerMetrics(BuildProducer<MetricsFactoryConsumerBuildItem> metrics,
            List<ContainerCreatedBuildItem> containers,
            IronJacamarBuildtimeConfig buildtimeConfig,
            Optional<MetricsCapabilityBuildItem> metricsCapability) {
        if (buildtimeConfig.metricsEnabled() && metricsCapability.isPresent()) {
            for (ContainerCreatedBuildItem container : containers) {
                boolean poolMetricsEnabled = true;
                var config = buildtimeConfig.resourceAdapters().get(container.identifier());
                if (config != null) {
                    poolMetricsEnabled = config.ra().cm().pool().enableMetrics().orElse(true);
                }
                if (poolMetricsEnabled) {
                    metrics.produce(new MetricsFactoryConsumerBuildItem(
                            new IronJacamarPoolMetricsConsumer(container.identifier())));
                }
            }
        }
    }
}
