package io.quarkiverse.ironjacamar.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;

import io.quarkus.arc.DefaultBean;

public class CachedConnectionManagerProducer {

    @Produces
    @ApplicationScoped
    @DefaultBean
    public CachedConnectionManager createCachedConnectionManager(TransactionIntegration transactionIntegration) {
        CachedConnectionManager ccm = new CachedConnectionManagerImpl(transactionIntegration);
        ccm.start();
        return ccm;
    }

    public void destroyCachedConnectionManager(@Disposes CachedConnectionManager ccm) {
        ccm.stop();
    }
}
