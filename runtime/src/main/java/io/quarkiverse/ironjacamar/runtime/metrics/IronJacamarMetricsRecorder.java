package io.quarkiverse.ironjacamar.runtime.metrics;

import java.util.function.Consumer;

import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.connectionmanager.pool.PoolStatisticsImpl;

import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.smallrye.common.annotation.Identifier;

@Recorder
public class IronJacamarMetricsRecorder {

    public Consumer<MetricsFactory> registerPoolMetrics(String resourceAdapterId) {
        return new Consumer<MetricsFactory>() {
            @Override
            public void accept(MetricsFactory metricsFactory) {
                IronJacamarContainer container = Arc.container().select(IronJacamarContainer.class,
                        Identifier.Literal.of(resourceAdapterId)).get();
                ConnectionManager connectionManager = container.getConnectionManager();

                PoolStatisticsImpl statistics = connectionManager.getPool().getInternalStatistics();

                metricsFactory.builder("ironjacamar.pool.active.count")
                        .description(
                                "Number of active connections")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildCounter(statistics::getActiveCount);

                metricsFactory.builder("ironjacamar.pool.available.count")
                        .description("Number of idle connections")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildCounter(statistics::getAvailableCount);

                metricsFactory.builder("ironjacamar.pool.average.blocking.time")
                        .description("Get the average time spent waiting on a connection (milliseconds)")
                        .unit("milliseconds")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getAverageBlockingTime);

                metricsFactory.builder("ironjacamar.pool.average.creation.time")
                        .description("Get the average time spent creating a connection (milliseconds)")
                        .unit("milliseconds")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getAverageCreationTime);

                metricsFactory.builder("ironjacamar.pool.average.get.time")
                        .description("Get the average time spent obtaining a connection (milliseconds)")
                        .unit("milliseconds")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getAverageGetTime);

                metricsFactory.builder("ironjacamar.pool.average.pool.time")
                        .description("Get the average time for a connection in the pool (milliseconds)")
                        .unit("milliseconds")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getAveragePoolTime);

                metricsFactory.builder("ironjacamar.pool.average.usage.time")
                        .description("Get the average time spent using a connection (milliseconds)")
                        .unit("milliseconds")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getAverageUsageTime);

                metricsFactory.builder("ironjacamar.pool.blocking.failure.count")
                        .description("Get the blocking failure count")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildCounter(statistics::getBlockingFailureCount);

                metricsFactory.builder("ironjacamar.pool.created.count")
                        .description("Get created count")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildCounter(statistics::getCreatedCount);

                metricsFactory.builder("ironjacamar.pool.destroyed.count")
                        .description("Get destroyed count")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildCounter(statistics::getDestroyedCount);

                metricsFactory.builder("ironjacamar.pool.idle.count")
                        .description("Get idle count")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildCounter(statistics::getIdleCount);

                metricsFactory.builder("ironjacamar.pool.in-use.count")
                        .description("Get in-use count")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildCounter(statistics::getInUseCount);

                metricsFactory.builder("ironjacamar.pool.max.creation.time")
                        .description("Get max creation time (milliseconds)")
                        .unit("milliseconds")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getMaxCreationTime);

                metricsFactory.builder("ironjacamar.pool.max.get.time")
                        .description("Get max get time (milliseconds)")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getMaxGetTime);

                metricsFactory.builder("ironjacamar.pool.max.pool.time")
                        .description("Get max pool time (milliseconds)")
                        .unit("milliseconds")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getMaxPoolTime);

                metricsFactory.builder("ironjacamar.pool.max.usage.time")
                        .description("Get max usage time (milliseconds)")
                        .unit("milliseconds")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getMaxUsageTime);

                metricsFactory.builder("ironjacamar.pool.max.used.count")
                        .description("Get max used count")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildCounter(statistics::getMaxUsedCount);

                metricsFactory.builder("ironjacamar.pool.max.wait.count")
                        .description("Get max wait count")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildCounter(statistics::getMaxWaitCount);

                metricsFactory.builder("ironjacamar.pool.max.wait.time")
                        .description("Get max wait time (milliseconds)")
                        .unit("milliseconds")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getMaxWaitTime);

                metricsFactory.builder("ironjacamar.pool.timeout.count")
                        .description("Get timed out")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildCounter(statistics::getTimedOut);

                metricsFactory.builder("ironjacamar.pool.total.blocking.time")
                        .description("Get the total time spent waiting on connections (milliseconds)")
                        .unit("milliseconds")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getTotalBlockingTime);

                metricsFactory.builder("ironjacamar.pool.total.creation.time")
                        .description("Get the total time spent creating connections (milliseconds)")
                        .unit("milliseconds")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getTotalCreationTime);

                metricsFactory.builder("ironjacamar.pool.total.get.time")
                        .description("Get the total time spent obtaining connections (milliseconds)")
                        .unit("milliseconds")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getTotalGetTime);

                metricsFactory.builder("ironjacamar.pool.total.pool.time")
                        .description("Get the total time for connections in the pool (milliseconds)")
                        .unit("milliseconds")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getTotalPoolTime);

                metricsFactory.builder("ironjacamar.pool.total.usage.time")
                        .description("Get the total time spent using connections (milliseconds)")
                        .unit("milliseconds")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildGauge(statistics::getTotalUsageTime);

                metricsFactory.builder("ironjacamar.pool.wait.count")
                        .description("Get wait count")
                        .tag("resourceAdapter", resourceAdapterId)
                        .buildCounter(statistics::getWaitCount);

            }
        };
    }
}
