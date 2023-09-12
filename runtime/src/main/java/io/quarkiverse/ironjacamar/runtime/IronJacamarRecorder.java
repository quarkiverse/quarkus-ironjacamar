package io.quarkiverse.ironjacamar.runtime;

import static io.quarkiverse.ironjacamar.Defaults.DEFAULT_BOOTSTRAP_CONTEXT_NAME;
import static io.quarkiverse.ironjacamar.Defaults.DEFAULT_WORK_MANAGER_NAME;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.resource.ResourceException;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.bootstrapcontext.BaseCloneableBootstrapContext;
import org.jboss.jca.core.bootstrapcontext.BootstrapContextCoordinator;
import org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.workmanager.WorkManagerCoordinator;
import org.jboss.jca.core.workmanager.WorkManagerImpl;

import io.quarkiverse.ironjacamar.runtime.security.QuarkusSecurityIntegration;
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

    public void initDefaultBoostrapContext() {
        ArcContainer container = Arc.container();
        TransactionIntegration transactionIntegration = container.select(TransactionIntegration.class).get();
        BaseCloneableBootstrapContext bootstrapContext = new BaseCloneableBootstrapContext();
        ManagedExecutor executorService = container.select(ManagedExecutor.class).get();

        // Create WorkManagerImpl
        WorkManagerImpl workManager = new WorkManagerImpl();
        workManager.setName(DEFAULT_WORK_MANAGER_NAME);
        workManager.setSpecCompliant(true);
        workManager.setSecurityIntegration(new QuarkusSecurityIntegration());

        // TODO: Should we have separate thread pools for short and long running tasks?
        workManager.setShortRunningThreadPool(executorService);
        workManager.setLongRunningThreadPool(executorService);

        bootstrapContext.setName(DEFAULT_BOOTSTRAP_CONTEXT_NAME);
        bootstrapContext.setWorkManager(workManager);
        bootstrapContext.setWorkManagerName(DEFAULT_WORK_MANAGER_NAME);
        bootstrapContext.setTransactionSynchronizationRegistry(transactionIntegration.getTransactionSynchronizationRegistry());
        bootstrapContext.setXATerminator(transactionIntegration.getXATerminator());

        // Register the default in the coordinator
        WorkManagerCoordinator.getInstance().setDefaultWorkManager(workManager);
        BootstrapContextCoordinator.getInstance().setDefaultBootstrapContext(bootstrapContext);
    }

    public RuntimeValue<Future<String>> initResourceAdapter(
            String key,
            Supplier<Vertx> vertxSupplier) {
        ArcContainer container = Arc.container();
        Vertx vertx = vertxSupplier.get();
        IronJacamarContainer ijContainer = container.select(IronJacamarContainer.class, Identifier.Literal.of(key)).get();
        CloneableBootstrapContext bootstrapContext = BootstrapContextCoordinator.getInstance().getDefaultBootstrapContext();
        IronJacamarVerticle verticle = new IronJacamarVerticle(key, ijContainer.getResourceAdapterFactory().getDescription(),
                ijContainer.getResourceAdapter(),
                bootstrapContext);
        Future<String> future = vertx.deployVerticle(verticle, new DeploymentOptions()
                .setWorkerPoolName("jca-worker-pool-" + key)
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
            IronJacamarSupport producer = container.select(IronJacamarSupport.class).get();
            producer.activateEndpoint(resourceAdapterId, activationSpecConfigId, endpointClassName, buildTimeConfig);
        });
    }
}
