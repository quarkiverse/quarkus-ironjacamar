package io.quarkiverse.ironjacamar.runtime;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.connectionmanager.TxConnectionManager;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkus.arc.Unremovable;
import io.quarkus.narayana.jta.runtime.TransactionManagerConfiguration;
import io.smallrye.common.annotation.Identifier;

/**
 * This class is a producer for {@link IronJacamarContainer} used in the {@link IronJacamarRecorder}
 * <p>
 * It is separated from the recorder because there are runtime configuration that are not available at build time.
 */
@Dependent
@Unremovable
public class IronJacamarSupport {

    @Inject
    IronJacamarRuntimeConfig runtimeConfig;

    @Inject
    TransactionManagerConfiguration transactionManagerConfig;

    @Inject
    ConnectionManagerFactory connectionManagerFactory;

    @Inject
    @Any
    Instance<IronJacamarContainer> containers;

    @Inject
    @Any
    Instance<ResourceAdapterFactory> resourceAdapterFactories;

    public IronJacamarContainer createContainer(String id, String kind) {
        ResourceAdapterFactory resourceAdapterFactory = resourceAdapterFactories.select(ResourceAdapterKind.Literal.of(kind))
                .get();
        var adapterRuntimeConfig = runtimeConfig.resourceAdapters().get(id);
        ResourceAdapter resourceAdapter;
        ManagedConnectionFactory managedConnectionFactory;
        ConnectionManager connectionManager;
        IronJacamarRuntimeConfig.ResourceAdapterConfig ra = adapterRuntimeConfig.ra();
        try {
            resourceAdapter = resourceAdapterFactory.createResourceAdapter(id, ra.config());
            managedConnectionFactory = resourceAdapterFactory.createManagedConnectionFactory(id, resourceAdapter);
            connectionManager = connectionManagerFactory.createConnectionManager(id, managedConnectionFactory, ra.cm());
            // Register recovery if enabled
            if (transactionManagerConfig.enableRecovery) {
                if (connectionManager instanceof TxConnectionManager) {
                    connectionManagerFactory.registerForRecovery(managedConnectionFactory,
                            (TxConnectionManager) connectionManager,
                            ra.cm().recovery());
                } else {
                    QuarkusIronJacamarLogger.log.connectionManagerNotTransactional(id);
                }
            }
        } catch (ResourceException re) {
            throw QuarkusIronJacamarLogger.log.cannotDeployResourceAdapter(re);
        }
        return new IronJacamarContainer(resourceAdapterFactory, resourceAdapter, managedConnectionFactory,
                connectionManager);
    }

    public void activateEndpoint(String containerId, String activationSpecConfigId, String endpointClassName,
            Map<String, String> buildTimeConfig, TransactionIntegration transactionIntegration) {
        IronJacamarContainer ijContainer = containers.select(Identifier.Literal.of(containerId)).get();
        Class<?> endpointClass;
        try {
            endpointClass = Class.forName(endpointClassName, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Map<String, String> config = new HashMap<>(buildTimeConfig);
        if (activationSpecConfigId != null) {
            var activationSpecConfig = runtimeConfig.activationSpecs().map().get(activationSpecConfigId);
            if (activationSpecConfig != null) {
                config.putAll(activationSpecConfig.config());
            }
        }
        try {
            ijContainer.endpointActivation(endpointClass, containerId, config,
                    transactionManagerConfig.enableRecovery ? transactionIntegration : null);
        } catch (ResourceException e) {
            throw QuarkusIronJacamarLogger.log.cannotActivateEndpoint(e);
        }
    }
}
