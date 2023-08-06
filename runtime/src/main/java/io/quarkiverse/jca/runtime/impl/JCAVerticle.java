package io.quarkiverse.jca.runtime.impl;

import java.util.Objects;

import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.XATerminator;
import jakarta.resource.spi.work.WorkManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.logging.Log;
import io.vertx.core.AbstractVerticle;

public final class JCAVerticle extends AbstractVerticle {
    private final ResourceAdapter ra;

    public JCAVerticle(ResourceAdapter resourceAdapter) {
        ra = Objects.requireNonNull(resourceAdapter);
    }

    @Override
    public void start() throws Exception {
        Log.infof("Starting JCA Resource Adapter %s", ra);
        WorkManager workManager = new VertxWorkManager(getVertx());
        // Lookup JTA resources
        ArcContainer container = Arc.container();
        TransactionSynchronizationRegistry registry = container.instance(TransactionSynchronizationRegistry.class).get();
        XATerminator xaTerminator = container.instance(XATerminator.class).get();
        // Create BootstrapContext
        BootstrapContext bootstrapContext = new DefaultBootstrapContext(workManager, registry, xaTerminator);
        try {
            ra.start(bootstrapContext);
        } catch (Exception e) {
            Log.error("ERROR while starting JCA Resource Adapter", e);
            throw e;
        }
    }

    @Override
    public void stop() {
        ra.stop();
    }
}
