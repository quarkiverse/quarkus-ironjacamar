package io.quarkiverse.ironjacamar.runtime;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

import org.jboss.jca.core.connectionmanager.TxConnectionManager;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.smallrye.common.annotation.Identifier;

/**
 * This class is a producer for {@link IronJacamarContainer} used in the {@link IronJacamarRecorder}
 * <p>
 * It is separated from the recorder because there are runtime configuration that are not available at build time.
 */
@Singleton
public class IronJacamarSupport {

    @Inject
    IronJacamarRuntimeConfig runtimeConfig;

    @Inject
    ConnectionManagerFactory connectionManagerFactory;

    public IronJacamarContainer createContainer(String id, String kind) {
        ResourceAdapterFactory resourceAdapterFactory = getResourceAdapterFactoryForKind(kind);
        var adapterRuntimeConfig = runtimeConfig.resourceAdapters().get(id);
        ResourceAdapter resourceAdapter;
        ManagedConnectionFactory managedConnectionFactory;
        try {
            resourceAdapter = resourceAdapterFactory.createResourceAdapter(adapterRuntimeConfig.ra().config());
            managedConnectionFactory = resourceAdapterFactory.createManagedConnectionFactory(resourceAdapter);
        } catch (ResourceException re) {
            throw new DeploymentException("Cannot deploy resource adapter", re);
        }
        TxConnectionManager connectionManager = connectionManagerFactory.createConnectionManager(managedConnectionFactory);
        return new IronJacamarContainer(resourceAdapterFactory, resourceAdapter, managedConnectionFactory,
                connectionManager);
    }

    public void activateEndpoint(String containerId, String activationSpecConfigId, String endpointClassName,
            Map<String, String> buildTimeConfig) {
        ArcContainer container = Arc.container();
        IronJacamarContainer ijContainer = container.select(IronJacamarContainer.class, Identifier.Literal.of(containerId))
                .get();
        Class<?> endpointClass;
        try {
            endpointClass = Class.forName(endpointClassName, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Map<String, String> config = new HashMap<>(buildTimeConfig);
        // TODO: May throw NPE?
        config.putAll(this.runtimeConfig.activationSpecs().map().get(activationSpecConfigId).config());
        try {
            ijContainer.endpointActivation(endpointClass, containerId, config);
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    private static ResourceAdapterFactory getResourceAdapterFactoryForKind(String kind) {
        ArcContainer container = Arc.container();
        ResourceAdapterFactory resourceAdapterFactory = container.select(ResourceAdapterFactory.class,
                ResourceAdapterKind.Literal.of(kind)).get();
        return resourceAdapterFactory;
    }

}
