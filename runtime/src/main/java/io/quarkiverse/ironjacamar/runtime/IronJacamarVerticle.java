package io.quarkiverse.ironjacamar.runtime;

import java.util.Collection;
import java.util.Objects;

import jakarta.resource.spi.ResourceAdapter;

import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.api.workmanager.WorkManager;

import io.quarkiverse.ironjacamar.runtime.listener.ResourceAdapterLifecycleListener;
import io.vertx.core.AbstractVerticle;

/**
 * A Vert.x {@link io.vertx.core.Verticle} that starts and stops a JCA {@link ResourceAdapter}.
 */
final class IronJacamarVerticle extends AbstractVerticle {

    private final String id;
    private final CloneableBootstrapContext bootstrapContext;
    private final IronJacamarContainer ironJacamarContainer;
    private final Collection<ResourceAdapterLifecycleListener> listeners;

    public IronJacamarVerticle(String id, IronJacamarContainer container, CloneableBootstrapContext bootstrapContext,
            Collection<ResourceAdapterLifecycleListener> listeners) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.ironJacamarContainer = Objects.requireNonNull(container, "container cannot be null");
        this.bootstrapContext = Objects.requireNonNull(bootstrapContext, "bootstrapContext cannot be null");
        this.listeners = Objects.requireNonNull(listeners, "listeners cannot be null");
    }

    @Override
    public void start() throws Exception {
        QuarkusIronJacamarLogger.log.startingResourceAdapter(id,
                ironJacamarContainer.resourceAdapterFactory().getDescription());
        ResourceAdapter resourceAdapter = ironJacamarContainer.resourceAdapter();
        for (ResourceAdapterLifecycleListener listener : listeners) {
            listener.preStartup(id, resourceAdapter);
        }
        resourceAdapter.start(bootstrapContext);
        for (ResourceAdapterLifecycleListener listener : listeners) {
            listener.postStartup(id, resourceAdapter);
        }
    }

    @Override
    public void stop() {
        QuarkusIronJacamarLogger.log.stoppingResourceAdapter(id);
        ResourceAdapter resourceAdapter = ironJacamarContainer.resourceAdapter();
        for (ResourceAdapterLifecycleListener listener : listeners) {
            listener.preShutdown(id, resourceAdapter);
        }
        resourceAdapter.stop();
        for (ResourceAdapterLifecycleListener listener : listeners) {
            listener.postShutdown(id, resourceAdapter);
        }
        // Shutdown the work manager
        ((WorkManager) bootstrapContext.getWorkManager()).shutdown();
        bootstrapContext.shutdown();
    }
}
