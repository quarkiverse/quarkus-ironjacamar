package io.quarkiverse.ironjacamar.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.ironjacamar.runtime.IronJacamarBuildtimeConfig;
import io.quarkiverse.ironjacamar.runtime.metrics.IronJacamarMetricsRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;

public class IronJacamarMetricsProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerMetrics(IronJacamarMetricsRecorder recorder,
            BuildProducer<MetricsFactoryConsumerBuildItem> metrics,
            List<ContainerCreatedBuildItem> containers,
            IronJacamarBuildtimeConfig buildtimeConfig,
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
                            recorder.registerPoolMetrics(container.identifier)));
                }
            }
        }
    }
}
