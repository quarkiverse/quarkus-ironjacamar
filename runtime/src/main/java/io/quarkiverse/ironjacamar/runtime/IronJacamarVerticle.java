package io.quarkiverse.ironjacamar.runtime;

import java.util.Objects;

import jakarta.resource.spi.ResourceAdapter;

import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.api.workmanager.WorkManager;

import io.vertx.core.AbstractVerticle;

/**
 * A Vert.x {@link io.vertx.core.Verticle} that starts and stops a JCA {@link ResourceAdapter}.
 */
final class IronJacamarVerticle extends AbstractVerticle {

    private final String id;
    private final String description;
    private final ResourceAdapter ra;
    private final CloneableBootstrapContext bootstrapContext;

    public IronJacamarVerticle(String id, String description, ResourceAdapter resourceAdapter,
            CloneableBootstrapContext bootstrapContext) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.ra = Objects.requireNonNull(resourceAdapter, "resourceAdapter cannot be null");
        this.bootstrapContext = Objects.requireNonNull(bootstrapContext, "bootstrapContext cannot be null");
    }

    @Override
    public void start() throws Exception {
        QuarkusIronJacamarLogger.log.startingResourceAdapter(id, description);
        ra.start(bootstrapContext);
    }

    @Override
    public void stop() {
        QuarkusIronJacamarLogger.log.stoppingResourceAdapter(id);
        ra.stop();
        // Shutdown the work manager
        ((WorkManager) bootstrapContext.getWorkManager()).shutdown();
        bootstrapContext.shutdown();
    }
}
