package io.quarkiverse.ironjacamar.runtime;

import java.util.Objects;

import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.XATerminator;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.logging.Logger;

import io.vertx.core.AbstractVerticle;

/**
 * A Vert.x {@link io.vertx.core.Verticle} that starts and stops a JCA {@link ResourceAdapter}.
 */
final class IronJacamarVerticle extends AbstractVerticle {

    private static final Logger log = Logger.getLogger(IronJacamarVerticle.class);

    private final ResourceAdapter ra;
    private final TransactionSynchronizationRegistry tsr;
    private final XATerminator xaTerminator;
    private QuarkusWorkManager workManager;

    public IronJacamarVerticle(ResourceAdapter resourceAdapter, TransactionSynchronizationRegistry tsr,
            XATerminator xaTerminator) {
        this.ra = Objects.requireNonNull(resourceAdapter, "resourceAdapter cannot be null");
        this.tsr = Objects.requireNonNull(tsr, "tsr cannot be null");
        this.xaTerminator = Objects.requireNonNull(xaTerminator, "xaTerminator cannot be null");

    }

    @Override
    public void start() throws Exception {
        log.infof("Starting JCA Resource Adapter %s", ra);
        workManager = new QuarkusWorkManager(vertx);
        // Create BootstrapContext
        BootstrapContext bootstrapContext = new QuarkusBootstrapContext(workManager, tsr, xaTerminator);
        ra.start(bootstrapContext);
    }

    @Override
    public void stop() {
        if (workManager != null) {
            workManager.close();
        }
        ra.stop();
    }
}
