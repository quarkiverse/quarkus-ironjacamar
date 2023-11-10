package io.quarkiverse.ironjacamar.runtime;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.connectionmanager.TxConnectionManager;

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
        IronJacamarRuntimeConfig.ResourceAdapterConfig ra = adapterRuntimeConfig.ra();
        try {
            resourceAdapter = resourceAdapterFactory.createResourceAdapter(id, ra.config());
            managedConnectionFactory = resourceAdapterFactory.createManagedConnectionFactory(id, resourceAdapter);
        } catch (ResourceException re) {
            throw new DeploymentException("Cannot deploy resource adapter", re);
        }
        ConnectionManager connectionManager = connectionManagerFactory.createConnectionManager(id, managedConnectionFactory,
                ra.cm());
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
        return new IronJacamarContainer(resourceAdapterFactory, resourceAdapter, managedConnectionFactory,
                connectionManager);
    }

    public void activateEndpoint(String containerId, String activationSpecConfigId, String endpointClassName,
            Map<String, String> buildTimeConfig) {
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
            ijContainer.endpointActivation(endpointClass, containerId, config);
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }
}
