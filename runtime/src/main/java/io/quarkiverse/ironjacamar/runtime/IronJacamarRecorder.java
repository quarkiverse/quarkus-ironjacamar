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
import org.jboss.jca.core.spi.security.SecurityIntegration;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.workmanager.WorkManagerCoordinator;
import org.jboss.jca.core.workmanager.WorkManagerImpl;

import io.quarkiverse.ironjacamar.runtime.security.QuarkusSecurityIntegration;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.runtime.BeanContainer;
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
        return context -> {
            IronJacamarSupport containerProducer = context
                    .getInjectedReference(IronJacamarSupport.class);
            return containerProducer.createContainer(id, kind);
        };
    }

    public Function<SyntheticCreationalContext<Object>, Object> createConnectionFactory(String id) {
        return context -> {
            IronJacamarContainer container = context.getInjectedReference(IronJacamarContainer.class,
                    Identifier.Literal.of(id));
            try {
                return container.createConnectionFactory();
            } catch (ResourceException e) {
                throw new DeploymentException("Cannot create connection factory", e);
            }
        };
    }

    public Function<SyntheticCreationalContext<CachedConnectionManager>, CachedConnectionManager> createCachedConnectionManager() {
        return context -> {
            TransactionIntegration ti = context.getInjectedReference(TransactionIntegration.class);
            CachedConnectionManagerImpl cachedConnectionManager = new CachedConnectionManagerImpl(ti);
            cachedConnectionManager.start();
            return cachedConnectionManager;
        };
    }

    public Function<SyntheticCreationalContext<QuarkusSecurityIntegration>, QuarkusSecurityIntegration> createSecurityIntegration() {
        return context -> new QuarkusSecurityIntegration();
    }

    public void initDefaultBootstrapContext(BeanContainer beanContainer) {
        TransactionIntegration transactionIntegration = beanContainer.beanInstance(TransactionIntegration.class);
        SecurityIntegration securityIntegration = beanContainer.beanInstance(QuarkusSecurityIntegration.class);
        BaseCloneableBootstrapContext bootstrapContext = new BaseCloneableBootstrapContext();
        ManagedExecutor executorService = beanContainer.beanInstance(ManagedExecutor.class);

        // Create WorkManagerImpl
        WorkManagerImpl workManager = new WorkManagerImpl();
        workManager.setName(DEFAULT_WORK_MANAGER_NAME);
        workManager.setSpecCompliant(true);
        workManager.setSecurityIntegration(securityIntegration);

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
            BeanContainer beanContainer,
            String key,
            Supplier<Vertx> vertxSupplier) {
        Vertx vertx = vertxSupplier.get();
        IronJacamarContainer ijContainer = beanContainer.beanInstance(IronJacamarContainer.class, Identifier.Literal.of(key));
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

    public void activateEndpoint(BeanContainer beanContainer,
            RuntimeValue<Future<String>> containerFuture,
            String resourceAdapterId,
            String activationSpecConfigId,
            String endpointClassName,
            Map<String, String> buildTimeConfig) {
        Future<String> future = containerFuture.getValue();
        future.onSuccess(s -> {
            IronJacamarSupport producer = beanContainer.beanInstance(IronJacamarSupport.class);
            TransactionIntegration ti = beanContainer.beanInstance(TransactionIntegration.class);
            producer.activateEndpoint(resourceAdapterId, activationSpecConfigId, endpointClassName, buildTimeConfig, ti);
        });
    }
}
