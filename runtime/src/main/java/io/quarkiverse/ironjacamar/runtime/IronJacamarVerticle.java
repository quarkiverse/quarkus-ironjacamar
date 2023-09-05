package io.quarkiverse.ironjacamar.runtime;

import java.util.Objects;

import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.XATerminator;
import jakarta.transaction.TransactionSynchronizationRegistry;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

/**
 * A Vert.x {@link io.vertx.core.Verticle} that starts and stops a JCA {@link ResourceAdapter}.
 */
final class IronJacamarVerticle extends AbstractVerticle {

    private final ResourceAdapter ra;
    private final TransactionSynchronizationRegistry tsr;
    private final XATerminator xaTerminator;
    private final String id;
    private final String description;

    private QuarkusWorkManager workManager;

    public IronJacamarVerticle(ResourceAdapter resourceAdapter, TransactionSynchronizationRegistry tsr,
            XATerminator xaTerminator, String id, String description) {
        this.ra = Objects.requireNonNull(resourceAdapter, "resourceAdapter cannot be null");
        this.tsr = Objects.requireNonNull(tsr, "tsr cannot be null");
        this.xaTerminator = Objects.requireNonNull(xaTerminator, "xaTerminator cannot be null");
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
    }

    @Override
    public void start() throws Exception {
        QuarkusIronJacamarLogger.log.startingResourceAdapter(id, description);
        workManager = new QuarkusWorkManager(vertx);
        // Create BootstrapContext
        BootstrapContext bootstrapContext = new QuarkusBootstrapContext(workManager, tsr, xaTerminator);
        ra.start(bootstrapContext);
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        if (workManager != null) {
            workManager.close().andThen((v) -> ra.stop()).andThen(stopPromise);
        } else {
            ra.stop();
            stopPromise.complete();
        }
    }
}
