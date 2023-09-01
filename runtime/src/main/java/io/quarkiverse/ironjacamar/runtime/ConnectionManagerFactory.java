package io.quarkiverse.ironjacamar.runtime;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.TransactionSupport;

import org.jboss.jca.common.api.metadata.common.FlushStrategy;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.connectionmanager.pool.PoolConfiguration;
import org.jboss.jca.core.connectionmanager.TxConnectionManager;
import org.jboss.jca.core.connectionmanager.pool.api.Pool;
import org.jboss.jca.core.connectionmanager.pool.api.PoolFactory;
import org.jboss.jca.core.connectionmanager.pool.api.PoolStrategy;
import org.jboss.jca.core.connectionmanager.pool.mcp.ManagedConnectionPoolFactory;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;

@Singleton
public class ConnectionManagerFactory {

    private final TransactionIntegration transactionIntegration;

    private final CachedConnectionManager ccm;

    @Inject
    public ConnectionManagerFactory(TransactionIntegration transactionIntegration, CachedConnectionManager ccm) {
        this.transactionIntegration = transactionIntegration;
        this.ccm = ccm;
    }

    public TxConnectionManager createConnectionManager(String id, ManagedConnectionFactory mcf) {
        Pool pool = new PoolFactory()
                .create(PoolStrategy.POOL_BY_CRI,
                        mcf,
                        new PoolConfiguration(),
                        false,
                        false,
                        ManagedConnectionPoolFactory.DEFAULT_IMPLEMENTATION);
        pool.setName("pool-" + id);
        return new org.jboss.jca.core.connectionmanager.ConnectionManagerFactory()
                .createTransactional(
                        TransactionSupport.TransactionSupportLevel.XATransaction,
                        pool,
                        null,
                        null,
                        true,
                        ccm,
                        false,
                        true,
                        true,
                        false,
                        null,
                        FlushStrategy.GRACEFULLY,
                        5,
                        1000L,
                        transactionIntegration,
                        true,
                        1000,
                        true,
                        true,
                        true);
    }
}
