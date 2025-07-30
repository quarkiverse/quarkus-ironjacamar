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

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkus.arc.Unremovable;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;

/**
 * This class is a producer for {@link IronJacamarContainer} used in the {@link IronJacamarRecorder}
 * <p>
 * It is separated from the recorder because there are runtime configuration that are not available at build time.
 */
@Dependent
@Unremovable
public class IronJacamarSupport {

    private final IronJacamarRuntimeConfig runtimeConfig;

    private final ConnectionManagerFactory connectionManagerFactory;

    private final TransactionRecoveryManager transactionRecoveryManager;

    private final Instance<IronJacamarContainer> containers;

    private final Instance<ResourceAdapterFactory> resourceAdapterFactories;

    /**
     * Constructor
     *
     * @param runtimeConfig The runtime configuration
     * @param connectionManagerFactory The connection manager factory
     * @param transactionRecoveryManager The transaction recovery manager
     * @param containers The containers
     * @param resourceAdapterFactories The resource adapter factories
     */
    @Inject
    public IronJacamarSupport(IronJacamarRuntimeConfig runtimeConfig, ConnectionManagerFactory connectionManagerFactory,
            TransactionRecoveryManager transactionRecoveryManager,
            @Any Instance<IronJacamarContainer> containers,
            @Any Instance<ResourceAdapterFactory> resourceAdapterFactories) {
        this.runtimeConfig = runtimeConfig;
        this.connectionManagerFactory = connectionManagerFactory;
        this.transactionRecoveryManager = transactionRecoveryManager;
        this.containers = containers;
        this.resourceAdapterFactories = resourceAdapterFactories;

    }

    /**
     * Create a container for the given resource adapter
     *
     * @param vertx
     * @param id The resource adapter id
     * @param kind The resource adapter kind
     * @return The container
     */
    public IronJacamarContainer createContainer(Vertx vertx, String id, String kind) {
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
            if (transactionRecoveryManager.isEnabled()) {
                if (connectionManager instanceof TxConnectionManager) {
                    transactionRecoveryManager.registerForRecovery(managedConnectionFactory,
                            (TxConnectionManager) connectionManager,
                            ra.cm().recovery().username().orElse(null),
                            ra.cm().recovery().password().orElse(null),
                            ra.cm().recovery().securityDomain().orElse(null));
                } else {
                    QuarkusIronJacamarLogger.log.connectionManagerNotTransactional(id);
                }
            }
        } catch (ResourceException re) {
            throw QuarkusIronJacamarLogger.log.cannotDeployResourceAdapter(re);
        }
        return new IronJacamarContainer(vertx, resourceAdapterFactory, resourceAdapter, managedConnectionFactory,
                connectionManager, transactionRecoveryManager);
    }

    /**
     * Activate an endpoint
     *
     * @param containerId The container id
     * @param activationSpecConfigId The activation spec config id
     * @param endpointClassName The endpoint class name
     * @param buildTimeConfig The build time config
     */
    public void activateEndpoint(String containerId, String activationSpecConfigId, String endpointClassName,
            Map<String, String> buildTimeConfig) {
        IronJacamarContainer ijContainer = containers.select(Identifier.Literal.of(containerId)).get();
        Class<?> endpointClass;
        try {
            endpointClass = Class.forName(endpointClassName, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        boolean enabled = true;
        Map<String, String> config = new HashMap<>(buildTimeConfig);
        if (activationSpecConfigId != null) {
            var activationSpecConfig = runtimeConfig.activationSpecs().map().get(activationSpecConfigId);
            if (activationSpecConfig != null) {
                enabled = activationSpecConfig.enabled();
                config.putAll(activationSpecConfig.config());
            }
        }
        if (enabled) {
            try {
                ijContainer.endpointActivation(endpointClass, containerId, config);
            } catch (ResourceException e) {
                throw QuarkusIronJacamarLogger.log.cannotActivateEndpoint(e);
            }
        }
    }

    /**
     * Activate an endpoint
     *
     * @param containerId The container id
     * @param activationSpecConfigId The activation spec config id
     * @param endpointClassName The endpoint class name
     * @param buildTimeConfig The build time config
     */
    public void activateEndpoint(String containerId, String activationSpecConfigId, Object endpointInstance,
            Map<String, String> buildTimeConfig) {
        IronJacamarContainer ijContainer = containers.select(Identifier.Literal.of(containerId)).get();
        boolean enabled = true;
        Map<String, String> config = new HashMap<>(buildTimeConfig);
        if (activationSpecConfigId != null) {
            var activationSpecConfig = runtimeConfig.activationSpecs().map().get(activationSpecConfigId);
            if (activationSpecConfig != null) {
                enabled = activationSpecConfig.enabled();
                config.putAll(activationSpecConfig.config());
            }
        }
        if (enabled) {
            try {
                ijContainer.endpointActivation(endpointInstance, containerId, config);
            } catch (ResourceException e) {
                throw QuarkusIronJacamarLogger.log.cannotActivateEndpoint(e);
            }
        }
    }
}
