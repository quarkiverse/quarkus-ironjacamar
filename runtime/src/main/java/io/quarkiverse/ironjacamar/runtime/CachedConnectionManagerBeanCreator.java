package io.quarkiverse.ironjacamar.runtime;

import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

/**
 * Creates the {@link CachedConnectionManager} synthetic bean.
 */
public class CachedConnectionManagerBeanCreator implements BeanCreator<CachedConnectionManager> {

    @Override
    public CachedConnectionManager create(SyntheticCreationalContext<CachedConnectionManager> context) {
        TransactionIntegration ti = context.getInjectedReference(TransactionIntegration.class);
        CachedConnectionManagerImpl cachedConnectionManager = new CachedConnectionManagerImpl(ti);
        cachedConnectionManager.start();
        return cachedConnectionManager;
    }
}
