package io.quarkiverse.ironjacamar.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.ironjacamar.runtime.IronJacamarBuildtimeConfig;
import io.quarkiverse.ironjacamar.runtime.metrics.IronJacamarMetricsRecorder;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;

/**
 * Processor that registers metrics for IronJacamar
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class IronJacamarMetricsProcessor {

    /**
     * Register metrics
     *
     * @param recorder the recorder
     * @param metrics the metrics
     * @param containers the containers
     * @param buildtimeConfig the buildtime config
     * @param beanContainerBuildItem the bean container
     * @param metricsCapability the metrics capability
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerMetrics(IronJacamarMetricsRecorder recorder,
            BuildProducer<MetricsFactoryConsumerBuildItem> metrics,
            List<ContainerCreatedBuildItem> containers,
            IronJacamarBuildtimeConfig buildtimeConfig,
            BeanContainerBuildItem beanContainerBuildItem,
            Optional<MetricsCapabilityBuildItem> metricsCapability) {
        if (buildtimeConfig.metricsEnabled() && metricsCapability.isPresent()) {
            for (ContainerCreatedBuildItem container : containers) {
                boolean poolMetricsEnabled = true;
                var config = buildtimeConfig.resourceAdapters().get(container.identifier);
                if (config != null) {
                    poolMetricsEnabled = config.ra().cm().pool().enableMetrics().orElse(true);
                }
                if (poolMetricsEnabled) {
                    metrics.produce(new MetricsFactoryConsumerBuildItem(
                            recorder.registerPoolMetrics(beanContainerBuildItem.getValue(), container.identifier)));
                }
            }
        }
    }
}
