package io.quarkiverse.ironjacamar.runtime;

import java.io.Closeable;
import java.util.Map;

import jakarta.enterprise.inject.Produces;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

import org.jboss.jca.core.connectionmanager.ConnectionManager;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.runtime.endpoint.DefaultMessageEndpointFactory;

/**
 * A managed bean that holds the resource adapter, managed connection factory and connection manager.
 */
public class IronJacamarContainer implements Closeable {

    private final ResourceAdapterFactory resourceAdapterFactory;
    private final ResourceAdapter resourceAdapter;
    private final ManagedConnectionFactory managedConnectionFactory;
    private final ConnectionManager connectionManager;

    public IronJacamarContainer(ResourceAdapterFactory resourceAdapterFactory,
            ResourceAdapter resourceAdapter,
            ManagedConnectionFactory managedConnectionFactory,
            ConnectionManager connectionManager) {
        this.resourceAdapterFactory = resourceAdapterFactory;
        this.resourceAdapter = resourceAdapter;
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = connectionManager;
    }

    @Produces
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    public Object createConnectionFactory() throws ResourceException {
        return managedConnectionFactory.createConnectionFactory(connectionManager);
    }

    public void endpointActivation(Class<?> endpointClass, String identifier, Map<String, String> config)
            throws ResourceException {
        ActivationSpec activationSpec = resourceAdapterFactory.createActivationSpec(resourceAdapter, endpointClass, config);
        DefaultMessageEndpointFactory messageEndpointFactory = new DefaultMessageEndpointFactory(endpointClass, identifier,
                resourceAdapterFactory);
        resourceAdapter.endpointActivation(messageEndpointFactory, activationSpec);
    }

    @Override
    public void close() {
        connectionManager.prepareShutdown();
        connectionManager.shutdown();
    }
}
