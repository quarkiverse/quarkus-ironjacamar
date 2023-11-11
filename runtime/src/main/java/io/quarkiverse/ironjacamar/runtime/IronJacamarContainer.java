package io.quarkiverse.ironjacamar.runtime;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.spi.transaction.recovery.XAResourceRecovery;

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

    /**
     * The active recovery modules
     */
    private final Set<XAResourceRecovery> recovery = new HashSet<>();

    public IronJacamarContainer(ResourceAdapterFactory resourceAdapterFactory,
            ResourceAdapter resourceAdapter,
            ManagedConnectionFactory managedConnectionFactory,
            ConnectionManager connectionManager) {
        this.resourceAdapterFactory = resourceAdapterFactory;
        this.resourceAdapter = resourceAdapter;
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = connectionManager;
    }

    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    public ResourceAdapterFactory getResourceAdapterFactory() {
        return resourceAdapterFactory;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public Object createConnectionFactory() throws ResourceException {
        return managedConnectionFactory.createConnectionFactory(connectionManager);
    }

    public void endpointActivation(Class<?> endpointClass, String identifier, Map<String, String> config,
            TransactionIntegration transactionIntegration)
            throws ResourceException {
        ActivationSpec activationSpec = resourceAdapterFactory.createActivationSpec(identifier, resourceAdapter, endpointClass,
                config);
        DefaultMessageEndpointFactory messageEndpointFactory = new DefaultMessageEndpointFactory(endpointClass, identifier,
                resourceAdapterFactory);
        resourceAdapter.endpointActivation(messageEndpointFactory, activationSpec);
        if (transactionIntegration != null) {
            XAResourceRecovery xrr = transactionIntegration.createXAResourceRecovery(resourceAdapter,
                    activationSpec,
                    resourceAdapterFactory.getProductName(),
                    resourceAdapterFactory.getProductVersion());
            try {
                xrr.initialize();
                transactionIntegration.getRecoveryRegistry().addXAResourceRecovery(xrr);
                recovery.add(xrr);
            } catch (Exception e) {
                throw QuarkusIronJacamarLogger.log.errorDuringRecoveryInitialization(e);
            }
        }
    }

    @Override
    public void close() {
        for (XAResourceRecovery xrr : recovery) {
            try {
                xrr.shutdown();
            } catch (Exception e) {
                QuarkusIronJacamarLogger.log.errorDuringRecoveryShutdown(e);
            }
        }
        recovery.clear();
        connectionManager.prepareShutdown();
        connectionManager.shutdown();
    }
}
