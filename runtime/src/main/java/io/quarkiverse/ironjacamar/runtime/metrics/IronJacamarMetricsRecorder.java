package io.quarkiverse.ironjacamar.runtime.metrics;

import java.util.function.Consumer;

import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.connectionmanager.pool.PoolStatisticsImpl;

import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.smallrye.common.annotation.Identifier;

@Recorder
public class IronJacamarMetricsRecorder {

    public Consumer<MetricsFactory> registerPoolMetrics(BeanContainer beanContainer, String resourceAdapterId) {
        return metricsFactory -> {
            IronJacamarContainer container = beanContainer.beanInstance(IronJacamarContainer.class,
                    Identifier.Literal.of(resourceAdapterId));
            ConnectionManager connectionManager = container.getConnectionManager();

            PoolStatisticsImpl statistics = connectionManager.getPool().getInternalStatistics();

            metricsFactory.builder("ironjacamar.pool.active.count")
                    .description(statistics.getDescription("ActiveCount"))
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildCounter(statistics::getActiveCount);

            metricsFactory.builder("ironjacamar.pool.available.count")
                    .description(statistics.getDescription("AvailableCount"))
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildCounter(statistics::getAvailableCount);

            metricsFactory.builder("ironjacamar.pool.average.blocking.time")
                    .description(statistics.getDescription("AverageBlockingTime"))
                    .unit("milliseconds")
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getAverageBlockingTime);

            metricsFactory.builder("ironjacamar.pool.average.creation.time")
                    .description(statistics.getDescription("AverageCreationTime"))
                    .unit("milliseconds")
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getAverageCreationTime);

            metricsFactory.builder("ironjacamar.pool.average.get.time")
                    .description(statistics.getDescription("AverageGetTime"))
                    .unit("milliseconds")
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getAverageGetTime);

            metricsFactory.builder("ironjacamar.pool.average.pool.time")
                    .description(statistics.getDescription("AveragePoolTime"))
                    .unit("milliseconds")
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getAveragePoolTime);

            metricsFactory.builder("ironjacamar.pool.average.usage.time")
                    .description(statistics.getDescription("AverageUsageTime"))
                    .unit("milliseconds")
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getAverageUsageTime);

            metricsFactory.builder("ironjacamar.pool.blocking.failure.count")
                    .description(statistics.getDescription("BlockingFailureCount"))
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildCounter(statistics::getBlockingFailureCount);

            metricsFactory.builder("ironjacamar.pool.created.count")
                    .description(statistics.getDescription("CreatedCount"))
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildCounter(statistics::getCreatedCount);

            metricsFactory.builder("ironjacamar.pool.destroyed.count")
                    .description(statistics.getDescription("DestroyedCount"))
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildCounter(statistics::getDestroyedCount);

            metricsFactory.builder("ironjacamar.pool.idle.count")
                    .description(statistics.getDescription("IdleCount"))
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildCounter(statistics::getIdleCount);

            metricsFactory.builder("ironjacamar.pool.in-use.count")
                    .description(statistics.getDescription("InUseCount"))
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildCounter(statistics::getInUseCount);

            metricsFactory.builder("ironjacamar.pool.max.creation.time")
                    .description(statistics.getDescription("MaxCreationTime"))
                    .unit("milliseconds")
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getMaxCreationTime);

            metricsFactory.builder("ironjacamar.pool.max.get.time")
                    .description(statistics.getDescription("MaxGetTime"))
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getMaxGetTime);

            metricsFactory.builder("ironjacamar.pool.max.pool.time")
                    .description(statistics.getDescription("MaxPoolTime"))
                    .unit("milliseconds")
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getMaxPoolTime);

            metricsFactory.builder("ironjacamar.pool.max.usage.time")
                    .description(statistics.getDescription("MaxUsageTime"))
                    .unit("milliseconds")
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getMaxUsageTime);

            metricsFactory.builder("ironjacamar.pool.max.used.count")
                    .description(statistics.getDescription("MaxUsedCount"))
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildCounter(statistics::getMaxUsedCount);

            metricsFactory.builder("ironjacamar.pool.max.wait.count")
                    .description(statistics.getDescription("MaxWaitCount"))
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildCounter(statistics::getMaxWaitCount);

            metricsFactory.builder("ironjacamar.pool.max.wait.time")
                    .description(statistics.getDescription("MaxWaitTime"))
                    .unit("milliseconds")
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getMaxWaitTime);

            metricsFactory.builder("ironjacamar.pool.timeout.count")
                    .description(statistics.getDescription("TimedOut"))
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildCounter(statistics::getTimedOut);

            metricsFactory.builder("ironjacamar.pool.total.blocking.time")
                    .description(statistics.getDescription("TotalBlockingTime"))
                    .unit("milliseconds")
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getTotalBlockingTime);

            metricsFactory.builder("ironjacamar.pool.total.creation.time")
                    .description(statistics.getDescription("TotalCreationTime"))
                    .unit("milliseconds")
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getTotalCreationTime);

            metricsFactory.builder("ironjacamar.pool.total.get.time")
                    .description(statistics.getDescription("TotalGetTime"))
                    .unit("milliseconds")
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getTotalGetTime);

            metricsFactory.builder("ironjacamar.pool.total.pool.time")
                    .description(statistics.getDescription("TotalPoolTime"))
                    .unit("milliseconds")
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getTotalPoolTime);

            metricsFactory.builder("ironjacamar.pool.total.usage.time")
                    .description(statistics.getDescription("TotalUsageTime"))
                    .unit("milliseconds")
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildGauge(statistics::getTotalUsageTime);

            metricsFactory.builder("ironjacamar.pool.wait.count")
                    .description(statistics.getDescription("WaitCount"))
                    .tag("resourceAdapter", resourceAdapterId)
                    .buildCounter(statistics::getWaitCount);

        };
    }
}
