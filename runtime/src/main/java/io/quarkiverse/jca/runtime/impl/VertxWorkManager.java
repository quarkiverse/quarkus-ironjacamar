package io.quarkiverse.jca.runtime.impl;

import jakarta.resource.spi.work.ExecutionContext;
import jakarta.resource.spi.work.Work;
import jakarta.resource.spi.work.WorkEvent;
import jakarta.resource.spi.work.WorkException;
import jakarta.resource.spi.work.WorkListener;
import jakarta.resource.spi.work.WorkManager;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

class VertxWorkManager implements WorkManager {

    private final Vertx vertx;

    public VertxWorkManager(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void doWork(Work work) throws WorkException {
        vertx.executeBlocking(new Handler<Promise<Object>>() {
            @Override
            public void handle(Promise<Object> event) {
                try {
                    work.run();
                    event.complete();
                } catch (Throwable t) {
                    event.fail(t);
                }
            }
        });
    }

    @Override
    public void doWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener)
            throws WorkException {
        vertx.setTimer(startTimeout, id -> {
            try {
                workListener.workStarted(new WorkEvent(this, WorkEvent.WORK_STARTED, work, null));
                work.run();
                workListener.workCompleted(new WorkEvent(this, WorkEvent.WORK_COMPLETED, work, null));
            } catch (Throwable t) {
                workListener.workRejected(new WorkEvent(this, WorkEvent.WORK_REJECTED, work, new WorkException(t)));
            }
        });
        workListener.workAccepted(new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null));
    }

    @Override
    public long startWork(Work work) throws WorkException {
        return 0;
    }

    @Override
    public long startWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener)
            throws WorkException {
        vertx.setPeriodic(1L, startTimeout, id -> {
            try {
                workListener.workStarted(new WorkEvent(this, WorkEvent.WORK_STARTED, work, null));
                work.run();
                workListener.workCompleted(new WorkEvent(this, WorkEvent.WORK_COMPLETED, work, null));
            } catch (Throwable t) {
                workListener.workRejected(new WorkEvent(this, WorkEvent.WORK_REJECTED, work, new WorkException(t)));
            }
        });
        workListener.workAccepted(new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null));
        return 0;
    }

    @Override
    public void scheduleWork(Work work) throws WorkException {
        vertx.runOnContext(id -> {
            work.run();
        });
    }

    @Override
    public void scheduleWork(Work work, long startTimeout,
            ExecutionContext execContext,
            WorkListener workListener)
            throws WorkException {
        vertx.setPeriodic(startTimeout, id -> {
            try {
                workListener.workStarted(new WorkEvent(this, WorkEvent.WORK_STARTED, work, null));
                work.run();
                workListener.workCompleted(new WorkEvent(this, WorkEvent.WORK_COMPLETED, work, null));
            } catch (Throwable t) {
                workListener.workRejected(new WorkEvent(this, WorkEvent.WORK_REJECTED, work, new WorkException(t)));
            }
        });
        workListener.workAccepted(new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null));
    }
}
