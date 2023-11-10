package io.quarkiverse.ironjacamar.runtime;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.TransactionSupport;

import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.connectionmanager.TxConnectionManager;
import org.jboss.jca.core.connectionmanager.pool.api.Pool;
import org.jboss.jca.core.connectionmanager.pool.api.PoolFactory;
import org.jboss.jca.core.connectionmanager.pool.mcp.ManagedConnectionPoolFactory;
import org.jboss.jca.core.spi.recovery.RecoveryPlugin;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.spi.transaction.XAResourceStatistics;
import org.jboss.jca.core.spi.transaction.recovery.XAResourceRecovery;

import io.quarkiverse.ironjacamar.runtime.IronJacamarRuntimeConfig.ConnectionManagerConfig.RecoveryConfig;

@Dependent
public class ConnectionManagerFactory {

    private final TransactionIntegration transactionIntegration;
    private final CachedConnectionManager ccm;
    private final RecoveryPlugin recoveryPlugin;

    @Inject
    public ConnectionManagerFactory(TransactionIntegration transactionIntegration, CachedConnectionManager ccm,
            RecoveryPlugin recoveryPlugin) {
        this.transactionIntegration = transactionIntegration;
        this.ccm = ccm;
        this.recoveryPlugin = recoveryPlugin;
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
        var factory = new org.jboss.jca.core.connectionmanager.ConnectionManagerFactory();
        if (config.transactionSupportLevel() == TransactionSupport.TransactionSupportLevel.NoTransaction) {
            return factory.createNonTransactional(
                    config.transactionSupportLevel(),
                    pool,
                    null,
                    null,
                    config.useCcm(),
                    ccm,
                    config.sharable(),
                    config.enlistment(),
                    config.connectable(),
                    config.tracking().orElse(Defaults.TRACKING),
                    config.flushStrategy(),
                    config.allocationRetry(),
                    config.allocationRetryWait().toMillis());
        } else {
            return factory
                    .createTransactional(
                            config.transactionSupportLevel(),
                            pool,
                            null,
                            null,
                            config.useCcm(),
                            ccm,
                            config.sharable(),
                            config.enlistment(),
                            config.connectable(),
                            config.tracking().orElse(Defaults.TRACKING),
                            new org.jboss.jca.core.api.management.ConnectionManager(id),
                            config.flushStrategy(),
                            config.allocationRetry(),
                            config.allocationRetryWait().toMillis(),
                            transactionIntegration,
                            config.interleaving(),
                            config.xaResourceTimeout().toSecondsPart(),
                            config.isSameRMOverride().orElse(Defaults.IS_SAME_RM_OVERRIDE),
                            config.wrapXAResource(),
                            config.padXid());
        }
    }

    public void registerForRecovery(ManagedConnectionFactory mcf, TxConnectionManager cm, RecoveryConfig config) {
        XAResourceRecovery xaResourceRecovery = transactionIntegration.createXAResourceRecovery(mcf,
                cm.getPadXid(),
                cm.getIsSameRMOverride(),
                cm.getWrapXAResource(),
                config.username().orElse(null),
                config.password().orElse(null),
                config.securityDomain().orElse(null),
                cm.getSubjectFactory(),
                recoveryPlugin,
                (XAResourceStatistics) cm.getPool().getStatistics());
        transactionIntegration.getRecoveryRegistry().addXAResourceRecovery(xaResourceRecovery);
    }
}
