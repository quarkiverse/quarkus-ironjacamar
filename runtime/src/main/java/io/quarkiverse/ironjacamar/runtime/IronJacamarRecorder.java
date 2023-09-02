package io.quarkiverse.ironjacamar.runtime;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.XATerminator;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.logging.Logger;

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

    public Function<SyntheticCreationalContext<IronJacamarContainer>, IronJacamarContainer> createContainerFunction(String id,
            String kind) {
        return new Function<>() {
            @Override
            public IronJacamarContainer apply(SyntheticCreationalContext<IronJacamarContainer> context) {
                IronJacamarSupport containerProducer = context
                        .getInjectedReference(IronJacamarSupport.class);
                return containerProducer.createContainer(id, kind);
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

    public Function<SyntheticCreationalContext<CachedConnectionManager>, CachedConnectionManager> createCachedConnectionManager() {
        return new Function<SyntheticCreationalContext<CachedConnectionManager>, CachedConnectionManager>() {
            @Override
            public CachedConnectionManager apply(SyntheticCreationalContext<CachedConnectionManager> context) {
                TransactionIntegration ti = context.getInjectedReference(TransactionIntegration.class);
                CachedConnectionManagerImpl cachedConnectionManager = new CachedConnectionManagerImpl(ti);
                cachedConnectionManager.start();
                return cachedConnectionManager;
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

    public void activateEndpoint(RuntimeValue<Future<String>> containerFuture,
            String resourceAdapterId,
            String activationSpecConfigId,
            String endpointClassName,
            Map<String, String> buildTimeConfig) {
        Future<String> future = containerFuture.getValue();
        future.onSuccess(s -> {
            ArcContainer container = Arc.container();
            IronJacamarSupport producer = container.instance(IronJacamarSupport.class).get();
            producer.activateEndpoint(resourceAdapterId, activationSpecConfigId, endpointClassName, buildTimeConfig);
        });
    }
}
