package io.quarkiverse.ironjacamar.runtime;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

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
    private final List<ActivatedEndpoint> activatedEndpoints = new ArrayList<>();

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
        QuarkusIronJacamarLogger.log.activatingEndpoint(endpointClass.getName());
        resourceAdapter.endpointActivation(messageEndpointFactory, activationSpec);
        activatedEndpoints.add(new ActivatedEndpoint(messageEndpointFactory, activationSpec));
        if (transactionRecoveryManager.isEnabled()) {
            transactionRecoveryManager.registerForRecovery(resourceAdapter, activationSpec,
                    resourceAdapterFactory.getProductName(), resourceAdapterFactory.getProductVersion());
        }
    }

    /**
     * Activate an endpoint with a pre-created listener instance.
     * <p>
     * Unlike {@link #endpointActivation(Class, String, Map)}, this method does not look up a CDI bean.
     * Instead, it uses the provided {@code listenerInstance} directly, making it suitable for
     * programmatic activation (e.g., from a Reactive Messaging connector).
     *
     * @param endpointClass The endpoint/listener interface class (e.g., {@code jakarta.jms.MessageListener})
     * @param identifier The resource adapter identifier
     * @param config The activation spec configuration
     * @param listenerInstance The listener instance that will receive messages from the resource adapter
     * @return A handle that can be used to deactivate the endpoint
     * @throws ResourceException if something goes wrong
     */
    public EndpointHandle endpointActivation(Class<?> endpointClass, String identifier,
            Map<String, String> config, Object listenerInstance) throws ResourceException {
        ActivationSpec activationSpec = resourceAdapterFactory.createActivationSpec(identifier, resourceAdapter, endpointClass,
                config);
        DefaultMessageEndpointFactory messageEndpointFactory = new DefaultMessageEndpointFactory(vertx, endpointClass,
                identifier, resourceAdapterFactory, listenerInstance);
        QuarkusIronJacamarLogger.log.activatingEndpoint(endpointClass.getName());
        resourceAdapter.endpointActivation(messageEndpointFactory, activationSpec);
        ActivatedEndpoint activated = new ActivatedEndpoint(messageEndpointFactory, activationSpec);
        activatedEndpoints.add(activated);
        return () -> {
            QuarkusIronJacamarLogger.log.deactivatingEndpoint(endpointClass.getName());
            resourceAdapter.endpointDeactivation(messageEndpointFactory, activationSpec);
            activatedEndpoints.remove(activated);
        };
    }

    /**
     * Called when the application shuts down
     */
    @Override
    public void close() {
        for (ActivatedEndpoint endpoint : activatedEndpoints) {
            QuarkusIronJacamarLogger.log.deactivatingEndpoint(endpoint.factory().getEndpointClass().getName());
            if (transactionRecoveryManager.isEnabled()) {
                transactionRecoveryManager.unregisterRecovery(endpoint.spec());
            }
            resourceAdapter.endpointDeactivation(endpoint.factory(), endpoint.spec());
        }
        activatedEndpoints.clear();
        connectionManager.prepareShutdown();
        connectionManager.shutdown();
    }

    /**
     * Holds a {@link MessageEndpointFactory} and its corresponding {@link ActivationSpec} for an activated endpoint,
     * so that {@link ResourceAdapter#endpointDeactivation} can be called with the same pair during shutdown.
     *
     * @param factory the message endpoint factory used during activation
     * @param spec the activation spec used during activation
     */
    private record ActivatedEndpoint(MessageEndpointFactory factory, ActivationSpec spec) {
    }
}
