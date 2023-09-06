package io.quarkiverse.ironjacamar.runtime;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.TransactionSupport;

import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.connectionmanager.pool.api.Pool;
import org.jboss.jca.core.connectionmanager.pool.api.PoolFactory;
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

    public ConnectionManager createConnectionManager(String id, ManagedConnectionFactory mcf,
            IronJacamarRuntimeConfig.ConnectionManagerConfig config) {
        var poolConfig = config.pool();
        Pool pool = new PoolFactory()
                .create(poolConfig.strategy(),
                        mcf,
                        poolConfig.config().toPoolConfiguration(),
                        poolConfig.noTxSeparatePool(),
                        poolConfig.sharable(),
                        ManagedConnectionPoolFactory.DEFAULT_IMPLEMENTATION);
        pool.setName("pool-" + id);
        return new org.jboss.jca.core.connectionmanager.ConnectionManagerFactory()
                .createTransactional(
                        TransactionSupport.TransactionSupportLevel.XATransaction,
                        pool,
                        null,
                        null,
                        Defaults.USE_CCM,
                        ccm,
                        Defaults.SHARABLE,
                        Defaults.ENLISTMENT,
                        Defaults.CONNECTABLE,
                        Defaults.TRACKING,
                        new org.jboss.jca.core.api.management.ConnectionManager(id),
                        config.flushStrategy(),
                        config.allocationRetry(),
                        config.allocationRetryWait().toMillis(),
                        transactionIntegration,
                        Defaults.INTERLEAVING,
                        config.xaResourceTimeout().toSecondsPart(),
                        Defaults.IS_SAME_RM_OVERRIDE,
                        Defaults.WRAP_XA_RESOURCE,
                        Defaults.PAD_XID);
    }
}
