package io.quarkiverse.ironjacamar.runtime;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.XATerminator;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.logging.Logger;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.runtime.endpoint.DefaultMessageEndpointFactory;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.runtime.ArcContainerSupplier;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.shutdown.ShutdownListener;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@Recorder
public class IronJacamarRecorder {

    private static final Logger log = Logger.getLogger(IronJacamarRecorder.class);

    public Function<SyntheticCreationalContext<IronJacamarContainer>, IronJacamarContainer> createContainerFunction(String kind,
            Map<String, String> config) {
        return new Function<>() {
            @Override
            public IronJacamarContainer apply(SyntheticCreationalContext<IronJacamarContainer> context) {
                ArcContainer container = Arc.container();
                ResourceAdapterFactory resourceAdapterFactory = container.select(ResourceAdapterFactory.class,
                        ResourceAdapterKind.Literal.of(kind)).get();
                ConnectionManagerFactory connectionManagerFactory = container.select(ConnectionManagerFactory.class).get();
                ResourceAdapter resourceAdapter;
                ManagedConnectionFactory managedConnectionFactory;
                try {
                    resourceAdapter = resourceAdapterFactory.createResourceAdapter(config);
                    managedConnectionFactory = resourceAdapterFactory.createManagedConnectionFactory(resourceAdapter);
                } catch (ResourceException re) {
                    throw new DeploymentException("Cannot deploy resource adapter", re);
                }
                ConnectionManager connectionManager = connectionManagerFactory
                        .createConnectionManager(managedConnectionFactory);
                return new IronJacamarContainer(resourceAdapter, managedConnectionFactory, connectionManager);
            }
        };
    }

    public ShutdownListener initResourceAdapter(
            String key,
            String kind,
            ArcContainerSupplier arcContainerSupplier,
            Supplier<Vertx> vertxSupplier)
            throws Exception {
        ArcContainer container = arcContainerSupplier.get();
        Vertx vertx = vertxSupplier.get();
        System.out.println("TOTAL -> " + container.listAll(IronJacamarContainer.class).size());
        IronJacamarContainer ijContainer = container.select(IronJacamarContainer.class,
                Identifier.Literal.of(key), ResourceAdapterKind.Literal.of(kind)).get();
        // Lookup JTA beans
        TransactionSynchronizationRegistry tsr = container.instance(TransactionSynchronizationRegistry.class).get();
        XATerminator xaTerminator = container.instance(XATerminator.class).get();
        IronJacamarVerticle verticle = new IronJacamarVerticle(ijContainer.getResourceAdapter(), tsr, xaTerminator);
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
            return new ResourceAdapterShutdownListener(ijContainer.getResourceAdapter());
            //            return activateEndpoints(resourceAdapter, adapterFactory);
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
                ActivationSpec activationSpec = resourceAdapterFactory.createActivationSpec(adapter, endpointClass, null);
                adapter.endpointActivation(messageEndpointFactory, activationSpec);
                endpointRegistry.registerEndpoint(messageEndpointFactory, activationSpec);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return endpointRegistry;
    }
}
