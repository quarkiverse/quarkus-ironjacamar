package io.quarkiverse.jca.runtime;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.XATerminator;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.resource.spi.work.WorkManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import io.quarkiverse.jca.runtime.endpoint.DefaultMessageEndpointFactory;
import io.quarkiverse.jca.runtime.spi.ResourceAdapterSupport;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.logging.Log;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.shutdown.ShutdownListener;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

@Recorder
public class JCARecorder {
    public RuntimeValue<ResourceAdapter> deployResourceAdapter(Supplier<Vertx> vertxSupplier, String resourceAdapterClassName) {
        ResourceAdapter resourceAdapter = null;
        Vertx vertx = vertxSupplier.get();
        try {
            Class<? extends ResourceAdapter> resourceAdapterClass = (Class<? extends ResourceAdapter>) Class
                    .forName(resourceAdapterClassName, true, Thread.currentThread().getContextClassLoader());
            // TODO: Check if class name matches
            try (InstanceHandle<? extends ResourceAdapter> instanceHandle = Arc.container().instance(resourceAdapterClass)) {
                resourceAdapter = instanceHandle.get();
                // Notify observers
                resourceAdapterSupport().configureResourceAdapter(resourceAdapter);
                Log.tracef("Deploying JCA Resource Adapter: %s ", resourceAdapterClassName);
                JCAVerticle verticle = new JCAVerticle(resourceAdapter);
                vertx.deployVerticle(verticle, new DeploymentOptions()
                        .setWorkerPoolName("jca-worker-pool")
                        .setWorkerPoolSize(1)
                        .setWorker(true));
            }
        } catch (Exception e) {
            //FIXME: bubble up the exception
            Log.error("Cannot deploy", e);
        }
        return new RuntimeValue<>(resourceAdapter);
    }

    public ShutdownListener activateEndpoints(RuntimeValue<ResourceAdapter> resourceAdapterRuntimeValue,
            Set<String> endpointClassNames) {
        ResourceAdapterSupport resourceAdapterSupport = resourceAdapterSupport();
        ResourceAdapter adapter = resourceAdapterRuntimeValue.getValue();
        ResourceAdapterShutdownListener endpointRegistry = new ResourceAdapterShutdownListener(adapter);
        for (String endpointClassName : endpointClassNames) {
            Class<?> endpointClass = null;
            try {
                endpointClass = Class.forName(endpointClassName, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            MessageEndpointFactory messageEndpointFactory = new DefaultMessageEndpointFactory(endpointClass,
                    resourceAdapterSupport);
            ActivationSpec activationSpec = resourceAdapterSupport.createActivationSpec(endpointClass);
            try {
                adapter.endpointActivation(messageEndpointFactory, activationSpec);
                endpointRegistry.registerEndpoint(messageEndpointFactory, activationSpec);
            } catch (ResourceException e) {
                throw new RuntimeException(e);
            }
        }
        return endpointRegistry;
    }

    private static ResourceAdapterSupport resourceAdapterSupport() {
        return Arc.container().instance(ResourceAdapterSupport.class).get();
    }

    static final class JCAVerticle extends AbstractVerticle {
        private final ResourceAdapter ra;

        public JCAVerticle(ResourceAdapter resourceAdapter) {
            ra = Objects.requireNonNull(resourceAdapter);
        }

        @Override
        public void start() throws Exception {
            Log.infof("Starting JCA Resource Adapter %s", ra);
            WorkManager workManager = new VertxWorkManager(vertx);
            // Lookup JTA resources
            ArcContainer container = Arc.container();
            TransactionSynchronizationRegistry registry = container.instance(TransactionSynchronizationRegistry.class).get();
            XATerminator xaTerminator = container.instance(XATerminator.class).get();
            // Create BootstrapContext
            BootstrapContext bootstrapContext = new DefaultBootstrapContext(workManager, registry, xaTerminator);
            ra.start(bootstrapContext);
        }

        @Override
        public void stop() {
            ra.stop();
        }
    }
}
