package io.quarkiverse.jca.runtime.impl;

import java.util.Timer;

import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.UnavailableException;
import jakarta.resource.spi.XATerminator;
import jakarta.resource.spi.work.WorkContext;
import jakarta.resource.spi.work.WorkManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

class DefaultBootstrapContext implements BootstrapContext {

    private final WorkManager workManager;
    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private final XATerminator xaTerminator;

    public DefaultBootstrapContext(WorkManager workManager,
            TransactionSynchronizationRegistry transactionSynchronizationRegistry,
            XATerminator xaTerminator) {
        this.workManager = workManager;
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        this.xaTerminator = xaTerminator;
    }

    @Override
    public WorkManager getWorkManager() {
        return workManager;
    }

    @Override
    public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return transactionSynchronizationRegistry;
    }

    @Override
    public XATerminator getXATerminator() {
        return xaTerminator;
    }

    @Override
    public Timer createTimer() throws UnavailableException {
        return new Timer("Quarkus JCA Timer", true);
    }

    @Override
    public boolean isContextSupported(Class<? extends WorkContext> workContextClass) {
        return false;
    }

}
