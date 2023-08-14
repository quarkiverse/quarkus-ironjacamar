package io.quarkiverse.jca.runtime.connection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.TransactionSupport;

import org.jboss.jca.common.api.metadata.common.FlushStrategy;
import org.jboss.jca.core.api.connectionmanager.pool.PoolConfiguration;
import org.jboss.jca.core.connectionmanager.ConnectionManagerFactory;
import org.jboss.jca.core.connectionmanager.pool.api.Pool;
import org.jboss.jca.core.connectionmanager.pool.api.PoolFactory;
import org.jboss.jca.core.connectionmanager.pool.api.PoolStrategy;
import org.jboss.jca.core.connectionmanager.pool.mcp.ManagedConnectionPoolFactory;
import org.jboss.jca.core.tx.jbossts.TransactionIntegrationImpl;

@ApplicationScoped
public class ConnectionManagerProducer {

    @Produces
    @ApplicationScoped
    public ConnectionManager createConnectionManager(Instance<ManagedConnectionFactory> factories,
            TransactionIntegrationImpl transactionIntegration) {
        ManagedConnectionFactory mcf = factories.get();
        Pool pool = new PoolFactory().create(PoolStrategy.ONE_POOL, mcf, new PoolConfiguration(), false, false,
                ManagedConnectionPoolFactory.DEFAULT_IMPLEMENTATION);
        return new ConnectionManagerFactory()
                .createTransactional(
                        TransactionSupport.TransactionSupportLevel.XATransaction,
                        pool,
                        null,
                        null,
                        false,
                        null,
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
        //        TxConnectionManagerImpl manager = new TxConnectionManagerImpl(transactionIntegration, false);
        //        OnePool onePool = new OnePool(mcf, new PoolConfiguration(), false, false,
        //                ManagedConnectionPoolFactory.DEFAULT_IMPLEMENTATION);
        //        manager.setPool(onePool);
        //        onePool.setConnectionManager(manager);
        //        return manager;
    }
}
