package io.quarkiverse.ironjacamar.runtime;

import static io.quarkiverse.ironjacamar.Defaults.DEFAULT_BOOTSTRAP_CONTEXT_NAME;
import static io.quarkiverse.ironjacamar.Defaults.DEFAULT_WORK_MANAGER_NAME;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.bootstrapcontext.BaseCloneableBootstrapContext;
import org.jboss.jca.core.bootstrapcontext.BootstrapContextCoordinator;
import org.jboss.jca.core.connectionmanager.pool.idle.IdleRemover;
import org.jboss.jca.core.connectionmanager.pool.validator.ConnectionValidator;
import org.jboss.jca.core.spi.security.SecurityIntegration;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.workmanager.WorkManagerCoordinator;
import org.jboss.jca.core.workmanager.WorkManagerImpl;

import io.quarkiverse.ironjacamar.runtime.IronJacamarRuntimeConfig.ConnectionManagerConfig.PoolConfig.PoolConfigurationConfig;
import io.quarkiverse.ironjacamar.runtime.listener.ResourceAdapterLifecycleListener;
import io.quarkiverse.ironjacamar.runtime.security.QuarkusSecurityIntegration;
import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.core.StartContext;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;

/**
 * Runtime startup logic for IronJacamar, invoked from the service actions registered at build time.
 */
public final class IronJacamarRecorder {

    private IronJacamarRecorder() {
    }

    /**
     * Initialize the default {@link CloneableBootstrapContext} and {@link WorkManagerImpl}
     *
     * @param beanContainer The bean container
     */
    public static void initDefaultBootstrapContext(BeanContainer beanContainer) {
        TransactionIntegration transactionIntegration = beanContainer.beanInstance(TransactionIntegration.class);
        SecurityIntegration securityIntegration = beanContainer.beanInstance(QuarkusSecurityIntegration.class);
        BaseCloneableBootstrapContext bootstrapContext = new BaseCloneableBootstrapContext();
        ManagedExecutor executorService = beanContainer.beanInstance(ManagedExecutor.class);

        // Create WorkManagerImpl
        WorkManagerImpl workManager = new WorkManagerImpl();
        workManager.setName(DEFAULT_WORK_MANAGER_NAME);
        workManager.setSpecCompliant(true);
        workManager.setSecurityIntegration(securityIntegration);
        workManager.setXATerminator(transactionIntegration.getXATerminator());

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
     * @param vertx The Vert.x instance
     * @param runtimeConfig The runtime configuration
     * @return An {@link IronJacamarContainerStart} holding the {@link Future} that completes when the resource adapter
     *         is initialized
     */
    public static IronJacamarContainerStart initResourceAdapter(
            BeanContainer beanContainer,
            String key,
            Vertx vertx,
            IronJacamarRuntimeConfig runtimeConfig) {
        IronJacamarContainer ijContainer = beanContainer.beanInstance(IronJacamarContainer.class, Identifier.Literal.of(key));
        CloneableBootstrapContext bootstrapContext = BootstrapContextCoordinator.getInstance().getDefaultBootstrapContext();
        List<ResourceAdapterLifecycleListener> listeners = Arc.container().select(ResourceAdapterLifecycleListener.class)
                .stream().toList();
        IronJacamarVerticle verticle = new IronJacamarVerticle(key, ijContainer, bootstrapContext, listeners);
        Duration maxWorkerExecuteTime = runtimeConfig.maxWorkerExecuteTime();
        Future<String> future = vertx.deployVerticle(verticle, new DeploymentOptions()
                .setWorkerPoolName("jca-worker-pool-" + key)
                .setWorkerPoolSize(1)
                .setThreadingModel(ThreadingModel.WORKER)
                .setMaxWorkerExecuteTime(maxWorkerExecuteTime.toMillis())
                .setMaxWorkerExecuteTimeUnit(TimeUnit.MILLISECONDS));
        return new IronJacamarContainerStart(future);
    }

    /**
     * Activate an endpoint once the resource adapter has started
     *
     * @param beanContainer The bean container
     * @param containerStart The container start holding the future that completes when the resource adapter is started
     * @param resourceAdapterId The resource adapter id
     * @param activationSpecConfigId The activation spec config id
     * @param endpointClassName The endpoint class name
     * @param buildTimeConfig The build time config
     */
    public static void activateEndpoint(BeanContainer beanContainer,
            IronJacamarContainerStart containerStart,
            String resourceAdapterId,
            String activationSpecConfigId,
            String endpointClassName,
            Map<String, String> buildTimeConfig) {
        containerStart.future().onSuccess(s -> {
            IronJacamarSupport producer = beanContainer.beanInstance(IronJacamarSupport.class);
            producer.activateEndpoint(resourceAdapterId, activationSpecConfigId, endpointClassName, buildTimeConfig);
        });
    }

    /**
     * Start the {@link IdleRemover} if any resource adapter pool requires it
     *
     * @param startContext The service start context
     * @param beanContainer The bean container
     * @param config The runtime configuration
     */
    public static void startIdleRemover(StartContext startContext, BeanContainer beanContainer,
            IronJacamarRuntimeConfig config) {
        boolean shouldStart = false;
        for (IronJacamarRuntimeConfig.ResourceAdapterOuterNamedConfig value : config.resourceAdapters().values()) {
            if (value.ra().cm().pool().config().idleTimeoutMinutes() > 0) {
                shouldStart = true;
                break;
            }
        }
        if (!shouldStart) {
            return;
        }
        try {
            QuarkusIronJacamarLogger.log.startIdleRemoverService();
            ManagedExecutor managedExecutor = beanContainer.beanInstance(ManagedExecutor.class);
            IdleRemover instance = IdleRemover.getInstance();
            instance.setExecutorService(managedExecutor);
            instance.start();
            startContext.onStop(() -> {
                try {
                    QuarkusIronJacamarLogger.log.stopIdleRemoverService();
                    instance.stop();
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            });
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Start the {@link ConnectionValidator} if any resource adapter pool requires it
     *
     * @param startContext The service start context
     * @param beanContainer The bean container
     * @param config The runtime configuration
     */
    public static void startConnectionValidator(StartContext startContext, BeanContainer beanContainer,
            IronJacamarRuntimeConfig config) {
        boolean shouldStart = false;
        for (IronJacamarRuntimeConfig.ResourceAdapterOuterNamedConfig value : config.resourceAdapters().values()) {
            PoolConfigurationConfig poolConfig = value.ra().cm().pool().config();
            if (poolConfig.backgroundValidation() && poolConfig.backgroundValidationMillis().orElse(0L) > 0) {
                shouldStart = true;
                break;
            }
        }
        if (!shouldStart) {
            return;
        }
        try {
            QuarkusIronJacamarLogger.log.startConnectionValidatorService();
            ManagedExecutor managedExecutor = beanContainer.beanInstance(ManagedExecutor.class);
            ConnectionValidator instance = ConnectionValidator.getInstance();
            instance.setExecutorService(managedExecutor);
            instance.start();
            startContext.onStop(() -> {
                try {
                    QuarkusIronJacamarLogger.log.stopConnectionValidatorService();
                    instance.stop();
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            });
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
