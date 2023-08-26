package io.quarkiverse.ironjacamar.runtime;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.XATerminator;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.jca.core.connectionmanager.TxConnectionManager;
import org.jboss.logging.Logger;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
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
                TxConnectionManager connectionManager = connectionManagerFactory
                        .createConnectionManager(managedConnectionFactory);
                return new IronJacamarContainer(resourceAdapterFactory, resourceAdapter, managedConnectionFactory,
                        connectionManager);
            }
        };
    }

    public Function<SyntheticCreationalContext<Object>, Object> createConnectionFactory(String id) {
        return new Function<SyntheticCreationalContext<Object>, Object>() {
            @Override
            public Object apply(SyntheticCreationalContext<Object> context) {
                IronJacamarContainer container = context.getInjectedReference(IronJacamarContainer.class,
                        Identifier.Literal.of(id));
                try {
                    return container.createConnectionFactory();
                } catch (ResourceException e) {
                    throw new DeploymentException("Cannot create connection factory", e);
                }
            }
        };
    }

    public RuntimeValue<Future<String>> initResourceAdapter(
            String key,
            Supplier<Vertx> vertxSupplier)
            throws Exception {
        ArcContainer container = Arc.container();
        Vertx vertx = vertxSupplier.get();
        IronJacamarContainer ijContainer = container.select(IronJacamarContainer.class, Identifier.Literal.of(key)).get();
        // Lookup JTA beans
        TransactionSynchronizationRegistry tsr = container.instance(TransactionSynchronizationRegistry.class).get();
        XATerminator xaTerminator = container.instance(XATerminator.class).get();
        IronJacamarVerticle verticle = new IronJacamarVerticle(ijContainer.getResourceAdapter(), tsr, xaTerminator);
        Future<String> future = vertx.deployVerticle(verticle, new DeploymentOptions()
                .setWorkerPoolName("jca-worker-pool")
                .setWorkerPoolSize(1)
                .setWorker(true));
        return new RuntimeValue<>(future);
    }

    public void activateEndpoint(RuntimeValue<Future<String>> futureRuntimeValue, String identifier, String endpointClassName,
            Map<String, String> config) {
        Future<String> future = futureRuntimeValue.getValue();
        future.onSuccess(s -> {
            ArcContainer container = Arc.container();
            IronJacamarContainer ijContainer = container.select(IronJacamarContainer.class, Identifier.Literal.of(identifier))
                    .get();
            Class<?> endpointClass = null;
            try {
                endpointClass = Class.forName(endpointClassName, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            try {
                ijContainer.endpointActivation(endpointClass, config);
            } catch (ResourceException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
