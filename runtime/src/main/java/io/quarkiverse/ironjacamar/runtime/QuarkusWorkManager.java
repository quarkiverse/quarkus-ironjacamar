package io.quarkiverse.ironjacamar.runtime;

import jakarta.resource.spi.work.ExecutionContext;
import jakarta.resource.spi.work.Work;
import jakarta.resource.spi.work.WorkEvent;
import jakarta.resource.spi.work.WorkException;
import jakarta.resource.spi.work.WorkListener;
import jakarta.resource.spi.work.WorkManager;

import io.quarkus.logging.Log;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;

/**
 * Use Vert.x worker threads to execute JCA Work instances.
 * <p>
 * Note: The {@link WorkManager} provided in IronJacamar depends on JBoss Threads 2.4.0.Final,
 * which is incompatible with Quarkus 3.x.
 */
class QuarkusWorkManager implements WorkManager {

    private final Vertx vertx;

    private final WorkerExecutor executor;

    public QuarkusWorkManager(Vertx vertx) {
        this.vertx = vertx;
        // TODO: Make the pool size configurable? Or use the Vert.x default?
        this.executor = vertx.createSharedWorkerExecutor("jca-work-manager", 5);
    }

    @Override
    public void doWork(Work work) throws WorkException {
        executor.executeBlocking(event -> {
            work.run();
        }, result -> {
            if (result.failed()) {
                Log.error("Failed to execute work", result.cause());
            }
        });
    }

    @Override
    public void doWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener)
            throws WorkException {
        vertx.setTimer(startTimeout, id -> {
            executor.executeBlocking(event -> {
                workListener.workStarted(new WorkEvent(this, WorkEvent.WORK_STARTED, work, null));
                work.run();
            }, result -> {
                if (result.succeeded()) {
                    workListener.workCompleted(new WorkEvent(this, WorkEvent.WORK_COMPLETED, work, null));
                } else {
                    Log.error("Failed to execute work", result.cause());
                    workListener.workRejected(
                            new WorkEvent(this, WorkEvent.WORK_REJECTED, work, new WorkException(result.cause())));
                }
            });
        });
        workListener.workAccepted(new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null));
    }

    @Override
    public long startWork(Work work) throws WorkException {
        doWork(work);
        return 0;
    }

    @Override
    public long startWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener)
            throws WorkException {
        vertx.setPeriodic(1L, startTimeout, id -> {
            executor.executeBlocking(event -> {
                workListener.workStarted(new WorkEvent(this, WorkEvent.WORK_STARTED, work, null));
                work.run();
            }, result -> {
                if (result.succeeded()) {
                    workListener.workCompleted(new WorkEvent(this, WorkEvent.WORK_COMPLETED, work, null));
                } else {
                    Log.error("Failed to execute work", result.cause());
                    workListener.workRejected(
                            new WorkEvent(this, WorkEvent.WORK_REJECTED, work, new WorkException(result.cause())));
                }
            });
        });
        workListener.workAccepted(new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null));
        return 0;
    }

    @Override
    public void scheduleWork(Work work) throws WorkException {
        executor.executeBlocking(event -> {
            work.run();
        }, result -> {
            if (result.failed()) {
                Log.error("Failed to execute work", result.cause());
            }
        });
    }

    @Override
    public void scheduleWork(Work work, long startTimeout,
            ExecutionContext execContext,
            WorkListener workListener)
            throws WorkException {
        vertx.setPeriodic(startTimeout, id -> {
            executor.executeBlocking(event -> {
                workListener.workStarted(new WorkEvent(this, WorkEvent.WORK_STARTED, work, null));
                work.run();
            }, result -> {
                if (result.succeeded()) {
                    workListener.workCompleted(new WorkEvent(this, WorkEvent.WORK_COMPLETED, work, null));
                } else {
                    Log.error("Failed to execute work", result.cause());
                    workListener.workRejected(
                            new WorkEvent(this, WorkEvent.WORK_REJECTED, work, new WorkException(result.cause())));
                }
            });
        });
        workListener.workAccepted(new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null));
    }

    public void close() {
        executor.close();
    }
}
