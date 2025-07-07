package io.quarkiverse.ironjacamar.runtime;

import static io.quarkiverse.ironjacamar.Defaults.DEFAULT_BOOTSTRAP_CONTEXT_NAME;
import static io.quarkiverse.ironjacamar.Defaults.DEFAULT_WORK_MANAGER_NAME;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.naming.Reference;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.bootstrapcontext.BaseCloneableBootstrapContext;
import org.jboss.jca.core.bootstrapcontext.BootstrapContextCoordinator;
import org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl;
import org.jboss.jca.core.spi.recovery.RecoveryPlugin;
import org.jboss.jca.core.spi.security.SecurityIntegration;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.workmanager.WorkManagerCoordinator;
import org.jboss.jca.core.workmanager.WorkManagerImpl;

import io.quarkiverse.ironjacamar.runtime.listener.ResourceAdapterLifecycleListener;
import io.quarkiverse.ironjacamar.runtime.security.QuarkusSecurityIntegration;
import io.quarkus.arc.Arc;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;

/**
 * The runtime recorder for IronJacamar
 */
@Recorder
public class IronJacamarRecorder {

    private final RuntimeValue<IronJacamarRuntimeConfig> runtimeConfig;

    /**
     * Constructor
     */
    public IronJacamarRecorder(RuntimeValue<IronJacamarRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    /**
     * Create an {@link IronJacamarContainer} for the given resource adapter
     *
     * @param id The resource adapter id
     * @param kind The resource adapter kind
     * @return A Function that creates an {@link IronJacamarContainer}
     */
    public Function<SyntheticCreationalContext<IronJacamarContainer>, IronJacamarContainer> createContainerFunction(String id,
            String kind) {
        return context -> {
            IronJacamarSupport containerProducer = context
                    .getInjectedReference(IronJacamarSupport.class);
            Vertx vertx = context
                    .getInjectedReference(Vertx.class);
            return containerProducer.createContainer(vertx, id, kind);
        };
    }

    /**
     * Create a connection factory from the given resource adapter (implementation vendor specific)
     *
     * @param id The resource adapter id
     * @return A function that creates a connection factory
     */
    public Function<SyntheticCreationalContext<Object>, Object> createConnectionFactory(String id) {
        return context -> {
            IronJacamarContainer container = context.getInjectedReference(IronJacamarContainer.class,
                    Identifier.Literal.of(id));
            try {
                Object connectionFactory = container.createConnectionFactory();
                if (connectionFactory instanceof Referenceable ref) {
                    // Set a reference to avoid serialization and JNDI lookups. See https://issues.apache.org/jira/browse/ARTEMIS-5253
                    ref.setReference(new Reference(connectionFactory.getClass().getCanonicalName(), null, null));
                }
                return connectionFactory;
            } catch (ResourceException e) {
                throw new DeploymentException("Cannot create connection factory", e);
            }
        };
    }

    /**
     * Create a {@link CachedConnectionManager}
     *
     * @return A function that creates a {@link CachedConnectionManager}
     */
    public Function<SyntheticCreationalContext<CachedConnectionManager>, CachedConnectionManager> createCachedConnectionManager() {
        return context -> {
            TransactionIntegration ti = context.getInjectedReference(TransactionIntegration.class);
            CachedConnectionManagerImpl cachedConnectionManager = new CachedConnectionManagerImpl(ti);
            cachedConnectionManager.start();
            return cachedConnectionManager;
        };
    }

    /**
     * Create a {@link QuarkusSecurityIntegration}
     *
     * @return A function that creates a {@link QuarkusSecurityIntegration}
     */
    public Function<SyntheticCreationalContext<QuarkusSecurityIntegration>, QuarkusSecurityIntegration> createSecurityIntegration() {
        return context -> new QuarkusSecurityIntegration();
    }

    /**
     * Create a {@link TransactionRecoveryManager}
     *
     * @return A function that creates a {@link TransactionRecoveryManager}
     */
    public Function<SyntheticCreationalContext<TransactionRecoveryManager>, TransactionRecoveryManager> createTransactionRecoveryManager() {
        return context -> {
            TransactionIntegration ti = context.getInjectedReference(TransactionIntegration.class);
            RecoveryPlugin recoveryPlugin = context.getInjectedReference(RecoveryPlugin.class);
            boolean enableRecovery = ConfigProvider.getConfig().getOptionalValue("quarkus.transaction-manager.enable-recovery",
                    Boolean.class).orElse(false);
            return new TransactionRecoveryManager(ti, recoveryPlugin, enableRecovery);
        };
    }

    /**
     * Initialize the default {@link CloneableBootstrapContext} and {@link WorkManagerImpl}
     *
     * @param beanContainer The bean container
     */
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

    /**
     * Initialize the resource adapter
     *
     * @param beanContainer The bean container
     * @param key The resource adapter key
     * @param vertxSupplier The vertx supplier
     * @return A {@link RuntimeValue} containing a {@link Future} that completes when the resource adapter is initialized
     */
    public RuntimeValue<Future<String>> initResourceAdapter(
            BeanContainer beanContainer,
            String key,
            Supplier<Vertx> vertxSupplier) {
        Vertx vertx = vertxSupplier.get();
        IronJacamarContainer ijContainer = beanContainer.beanInstance(IronJacamarContainer.class, Identifier.Literal.of(key));
        CloneableBootstrapContext bootstrapContext = BootstrapContextCoordinator.getInstance().getDefaultBootstrapContext();
        List<ResourceAdapterLifecycleListener> listeners = Arc.container().select(ResourceAdapterLifecycleListener.class)
                .stream().toList();
        IronJacamarVerticle verticle = new IronJacamarVerticle(key, ijContainer, bootstrapContext, listeners);
        Duration maxWorkerExecuteTime = runtimeConfig.getValue().maxWorkerExecuteTime();
        Future<String> future = vertx.deployVerticle(verticle, new DeploymentOptions()
                .setWorkerPoolName("jca-worker-pool-" + key)
                .setWorkerPoolSize(1)
                .setThreadingModel(ThreadingModel.WORKER)
                .setMaxWorkerExecuteTime(maxWorkerExecuteTime.toMillis())
                .setMaxWorkerExecuteTimeUnit(TimeUnit.MILLISECONDS));
        return new RuntimeValue<>(future);
    }

    /**
     * Activate an endpoint
     *
     * @param beanContainer The bean container
     * @param containerFuture The container future
     * @param resourceAdapterId The resource adapter id
     * @param activationSpecConfigId The activation spec config id
     * @param endpointClassName The endpoint class name
     * @param buildTimeConfig The build time config
     */
    public void activateEndpoint(BeanContainer beanContainer,
            RuntimeValue<Future<String>> containerFuture,
            String resourceAdapterId,
            String activationSpecConfigId,
            String endpointClassName,
            Map<String, String> buildTimeConfig) {
        Future<String> future = containerFuture.getValue();
        future.onSuccess(s -> {
            IronJacamarSupport producer = beanContainer.beanInstance(IronJacamarSupport.class);
            producer.activateEndpoint(resourceAdapterId, activationSpecConfigId, endpointClassName, buildTimeConfig);
        });
    }
}
