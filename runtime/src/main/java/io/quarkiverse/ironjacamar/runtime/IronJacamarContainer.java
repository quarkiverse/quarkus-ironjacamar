package io.quarkiverse.ironjacamar.runtime;

import java.io.Closeable;

import jakarta.enterprise.inject.Produces;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

import org.jboss.jca.core.connectionmanager.TxConnectionManager;

/**
 * A managed bean that holds the resource adapter, managed connection factory and connection manager.
 */
public class IronJacamarContainer implements Closeable {

    private final ResourceAdapter resourceAdapter;
    private final ManagedConnectionFactory managedConnectionFactory;
    private final TxConnectionManager connectionManager;

    public IronJacamarContainer(ResourceAdapter resourceAdapter,
            ManagedConnectionFactory managedConnectionFactory,
            TxConnectionManager connectionManager) {
        this.resourceAdapter = resourceAdapter;
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = connectionManager;
    }

    @Produces
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    @Override
    public void close() {
        connectionManager.prepareShutdown();
        connectionManager.shutdown();
    }
}
