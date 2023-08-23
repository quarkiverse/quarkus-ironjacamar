package io.quarkiverse.ironjacamar.runtime;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.XATerminator;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.runtime.endpoint.DefaultMessageEndpointFactory;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.shutdown.ShutdownListener;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@Recorder
public class IronJacamarRecorder {

    private static final Logger log = Logger.getLogger(IronJacamarRecorder.class);

    public ShutdownListener initResourceAdapter(Supplier<Vertx> vertxSupplier, Supplier<ResourceAdapterFactory> factorySupplier,
            Supplier<Config> configSupplier)
            throws Exception {
        Vertx vertx = vertxSupplier.get();
        ResourceAdapterFactory adapterFactory = factorySupplier.get();
        Config config = configSupplier.get();
        //TODO: Config
        final ResourceAdapter resourceAdapter = adapterFactory.createResourceAdapter(null);
        JCAVerticle verticle = new JCAVerticle(resourceAdapter);
        //TODO: maybe there is a better way to do this
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean started = new AtomicBoolean();
        vertx.deployVerticle(verticle, new DeploymentOptions()
                .setWorkerPoolName("jca-worker-pool")
                .setWorkerPoolSize(1)
                .setWorker(true),
                new Handler<AsyncResult<String>>() {
                    @Override
                    public void handle(AsyncResult<String> event) {
                        started.set(event.succeeded());
                        if (event.failed()) {
                            log.errorf(event.cause(), "Failed to deploy JCA Resource Adapter: %s ", event.result());
                        }
                        latch.countDown();
                    }
                });
        latch.await();
        if (started.get()) {
            return activateEndpoints(resourceAdapter, adapterFactory);
        }
        return null;
    }

    private ShutdownListener activateEndpoints(ResourceAdapter adapter,
            ResourceAdapterFactory resourceAdapterFactory) {
        //TODO: Find the respective endpoints
        Set<String> endpointClassNames = new HashSet<>();
        ResourceAdapterShutdownListener endpointRegistry = new ResourceAdapterShutdownListener(adapter);
        for (String endpointClassName : endpointClassNames) {
            Class<?> endpointClass = null;
            try {
                endpointClass = Class.forName(endpointClassName, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            MessageEndpointFactory messageEndpointFactory = new DefaultMessageEndpointFactory(endpointClass,
                    resourceAdapterFactory);
            try {
                ActivationSpec activationSpec = resourceAdapterFactory.createActivationSpec(null, adapter, endpointClass);
                adapter.endpointActivation(messageEndpointFactory, activationSpec);
                endpointRegistry.registerEndpoint(messageEndpointFactory, activationSpec);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return endpointRegistry;
    }

    static final class JCAVerticle extends AbstractVerticle {
        private final ResourceAdapter ra;
        private QuarkusWorkManager workManager;

        public JCAVerticle(ResourceAdapter resourceAdapter) {
            ra = Objects.requireNonNull(resourceAdapter);
        }

        @Override
        public void start() throws Exception {
            log.infof("Starting JCA Resource Adapter %s", ra);
            workManager = new QuarkusWorkManager(vertx);
            // Lookup JTA resources
            ArcContainer container = Arc.container();
            TransactionSynchronizationRegistry registry = container.instance(TransactionSynchronizationRegistry.class).get();
            XATerminator xaTerminator = container.instance(XATerminator.class).get();
            // Create BootstrapContext
            BootstrapContext bootstrapContext = new QuarkusBootstrapContext(workManager, registry, xaTerminator);
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
}
