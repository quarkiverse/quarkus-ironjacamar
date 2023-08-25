package io.quarkiverse.ironjacamar.runtime;

import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

/**
 * A managed bean that holds the resource adapter, managed connection factory and connection manager.
 */
public class IronJacamarContainer {
    private final ResourceAdapter resourceAdapter;
    private final ManagedConnectionFactory managedConnectionFactory;
    private final ConnectionManager connectionManager;

    public IronJacamarContainer(ResourceAdapter resourceAdapter,
            ManagedConnectionFactory managedConnectionFactory,
            ConnectionManager connectionManager) {
        this.resourceAdapter = resourceAdapter;
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = connectionManager;
    }

    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    public ManagedConnectionFactory getManagedConnectionFactory() {
        return managedConnectionFactory;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }
}
