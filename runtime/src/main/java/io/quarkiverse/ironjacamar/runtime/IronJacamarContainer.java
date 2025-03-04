package io.quarkiverse.ironjacamar.runtime;

import java.io.Closeable;
import java.util.Map;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

import org.jboss.jca.core.connectionmanager.ConnectionManager;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.runtime.endpoint.DefaultMessageEndpointFactory;
import io.vertx.core.Vertx;

/**
 * A managed bean that holds the resource adapter, managed connection factory and connection manager.
 */
public class IronJacamarContainer implements Closeable {

    private final Vertx vertx;
    private final ResourceAdapterFactory resourceAdapterFactory;
    private final ResourceAdapter resourceAdapter;
    private final ManagedConnectionFactory managedConnectionFactory;
    private final ConnectionManager connectionManager;
    private final TransactionRecoveryManager transactionRecoveryManager;

    /**
     * Constructor
     *
     * @param vertx
     * @param resourceAdapterFactory The resource adapter factory
     * @param resourceAdapter The resource adapter
     * @param managedConnectionFactory The managed connection factory
     * @param connectionManager The connection manager
     * @param transactionRecoveryManager The transaction recovery manager
     */
    public IronJacamarContainer(Vertx vertx, ResourceAdapterFactory resourceAdapterFactory,
            ResourceAdapter resourceAdapter,
            ManagedConnectionFactory managedConnectionFactory,
            ConnectionManager connectionManager,
            TransactionRecoveryManager transactionRecoveryManager) {
        this.vertx = vertx;
        this.resourceAdapterFactory = resourceAdapterFactory;
        this.resourceAdapter = resourceAdapter;
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = connectionManager;
        this.transactionRecoveryManager = transactionRecoveryManager;
    }

    /**
     * Get the resource adapter
     *
     * @return The resource adapter
     */
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    /**
     * Get the managed connection factory
     *
     * @return The {@link ResourceAdapterFactory}
     */
    public ResourceAdapterFactory getResourceAdapterFactory() {
        return resourceAdapterFactory;
    }

    /**
     * Get the connection manager
     *
     * @return The {@link ConnectionManager} instance
     */
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    /**
     * Create a connection factory. It will be managed by the connection manager.
     *
     * @return The connection factory
     * @throws ResourceException if something goes wrong
     */
    public Object createConnectionFactory() throws ResourceException {
        return managedConnectionFactory.createConnectionFactory(connectionManager);
    }

    /**
     * Activate an endpoint
     *
     * @param endpointClass The endpoint class
     * @param identifier The identifier
     * @param config The configuration
     * @throws ResourceException if something goes wrong
     */
    public void endpointActivation(Class<?> endpointClass, String identifier, Map<String, String> config)
            throws ResourceException {
        ActivationSpec activationSpec = resourceAdapterFactory.createActivationSpec(identifier, resourceAdapter, endpointClass,
                config);
        DefaultMessageEndpointFactory messageEndpointFactory = new DefaultMessageEndpointFactory(vertx, endpointClass,
                identifier,
                resourceAdapterFactory);
        resourceAdapter.endpointActivation(messageEndpointFactory, activationSpec);
        if (transactionRecoveryManager.isEnabled()) {
            transactionRecoveryManager.registerForRecovery(resourceAdapter, activationSpec,
                    resourceAdapterFactory.getProductName(), resourceAdapterFactory.getProductVersion());
        }
    }

    /**
     * Called when the application shuts down
     */
    @Override
    public void close() {
        connectionManager.prepareShutdown();
        connectionManager.shutdown();
    }
}
