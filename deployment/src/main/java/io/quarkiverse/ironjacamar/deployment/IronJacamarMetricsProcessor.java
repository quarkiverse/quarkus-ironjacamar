package io.quarkiverse.ironjacamar.deployment;

import java.util.List;

import io.quarkiverse.ironjacamar.runtime.IronJacamarBuildtimeConfig;
import io.quarkiverse.ironjacamar.runtime.metrics.IronJacamarMetricsRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;

public class IronJacamarMetricsProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerMetrics(IronJacamarMetricsRecorder recorder,
            BuildProducer<MetricsFactoryConsumerBuildItem> metrics,
            List<ContainerCreatedBuildItem> containers,
            IronJacamarBuildtimeConfig buildtimeConfig) {
        for (ContainerCreatedBuildItem container : containers) {
            var config = buildtimeConfig.resourceAdapters().get(container.identifier);
            if (config == null) {
                continue;
            }
            if (config.ra().connectionManager().pool().metricsEnabled()) {
                metrics.produce(new MetricsFactoryConsumerBuildItem(
                        recorder.registerPoolMetrics(container.identifier)));
            }
        }
    }
}
