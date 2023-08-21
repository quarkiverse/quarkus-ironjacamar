package io.quarkiverse.ironjacamar.runtime.connectionmanager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.TransactionSupport;

import org.jboss.jca.common.api.metadata.common.FlushStrategy;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.connectionmanager.pool.PoolConfiguration;
import org.jboss.jca.core.connectionmanager.ConnectionManagerFactory;
import org.jboss.jca.core.connectionmanager.TxConnectionManager;
import org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl;
import org.jboss.jca.core.connectionmanager.pool.api.Pool;
import org.jboss.jca.core.connectionmanager.pool.api.PoolFactory;
import org.jboss.jca.core.connectionmanager.pool.api.PoolStrategy;
import org.jboss.jca.core.connectionmanager.pool.mcp.ManagedConnectionPoolFactory;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;

import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class ConnectionManagerProducer {

    @Produces
    @ApplicationScoped
    @DefaultBean
    public TxConnectionManager createConnectionManager(
            Instance<ManagedConnectionFactory> factories,
            TransactionIntegration transactionIntegration,
            CachedConnectionManager ccm) {
        ManagedConnectionFactory mcf = factories.get();
        Pool pool = new PoolFactory().create(PoolStrategy.ONE_POOL, mcf, new PoolConfiguration(), false, false,
                ManagedConnectionPoolFactory.DEFAULT_IMPLEMENTATION);
        return new ConnectionManagerFactory()
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

    public void destroyConnectionManager(@Disposes TxConnectionManager connectionManager) {
        connectionManager.prepareShutdown();
        connectionManager.shutdown();
    }
}
