package io.quarkiverse.ironjacamar.runtime;

import static io.quarkiverse.ironjacamar.Defaults.DEFAULT_BOOTSTRAP_CONTEXT_NAME;
import static io.quarkiverse.ironjacamar.Defaults.DEFAULT_WORK_MANAGER_NAME;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
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
import io.quarkus.core.AsyncStartContext;
import io.quarkus.core.StartContext;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.DeploymentOptions;
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
        BaseCloneableBootstrapContext bootstrapContext = new BaseCloneableBootstrapContext();

        // Create WorkManagerImpl
        WorkManagerImpl workManager = createWorkManager(beanContainer, null);

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
     * Create a {@link WorkManagerImpl} running its work on the Quarkus {@link ManagedExecutor}
     *
     * @param beanContainer The bean container
     * @param id The id of the work manager, {@code null} for the default one
     * @return The work manager
     */
    private static WorkManagerImpl createWorkManager(BeanContainer beanContainer, String id) {
        TransactionIntegration transactionIntegration = beanContainer.beanInstance(TransactionIntegration.class);
        SecurityIntegration securityIntegration = beanContainer.beanInstance(QuarkusSecurityIntegration.class);
        ManagedExecutor executorService = beanContainer.beanInstance(ManagedExecutor.class);

        WorkManagerImpl workManager = new WorkManagerImpl();
        workManager.setName(DEFAULT_WORK_MANAGER_NAME);
        if (id != null) {
            workManager.setId(id);
        }
        workManager.setSpecCompliant(true);
        workManager.setSecurityIntegration(securityIntegration);
        workManager.setXATerminator(transactionIntegration.getXATerminator());

        // TODO: Should we have separate thread pools for short and long running tasks?
        workManager.setShortRunningThreadPool(executorService);
        workManager.setLongRunningThreadPool(executorService);
        return workManager;
    }

    /**
     * Initialize the resource adapter
     * <p>
     * The service completes once the verticle running the resource adapter is deployed, and fails when the
     * resource adapter fails to start, so that the application does not start without it.
     *
     * @param startContext The context of the asynchronous service start
     * @param beanContainer The bean container
     * @param key The resource adapter key
     * @param vertx The Vert.x instance
     * @param runtimeConfig The runtime configuration
     */
    public static void initResourceAdapter(
            AsyncStartContext<IronJacamarContainerStart> startContext,
            BeanContainer beanContainer,
            String key,
            Vertx vertx,
            IronJacamarRuntimeConfig runtimeConfig) {
        IronJacamarContainer ijContainer = beanContainer.beanInstance(IronJacamarContainer.class, Identifier.Literal.of(key));
        // Each resource adapter gets its own bootstrap context, cloned from the default one, because it is shut
        // down when the resource adapter stops
        CloneableBootstrapContext bootstrapContext = BootstrapContextCoordinator.getInstance().createBootstrapContext(key);
        // The work manager the coordinator cloned shares the shutdown state and the set of active work of the
        // default one, so stopping a resource adapter would shut the work manager of the others down: give this
        // resource adapter a work manager of its own instead
        bootstrapContext.setWorkManager(createWorkManager(beanContainer, key));
        List<ResourceAdapterLifecycleListener> listeners = Arc.container().select(ResourceAdapterLifecycleListener.class)
                .stream().toList();
        IronJacamarVerticle verticle = new IronJacamarVerticle(key, ijContainer, bootstrapContext, listeners);
        Duration maxWorkerExecuteTime = runtimeConfig.maxWorkerExecuteTime();
        vertx.deployVerticle(verticle, new DeploymentOptions()
                .setWorkerPoolName("jca-worker-pool-" + key)
                .setWorkerPoolSize(1)
                .setThreadingModel(ThreadingModel.WORKER)
                .setMaxWorkerExecuteTime(maxWorkerExecuteTime.toMillis())
                .setMaxWorkerExecuteTimeUnit(TimeUnit.MILLISECONDS))
                .onSuccess(deploymentId -> startContext.startComplete(new IronJacamarContainerStart(deploymentId)))
                .onFailure(throwable -> {
                    if (failOnStartError(runtimeConfig, key)) {
                        startContext.startFailed(QuarkusIronJacamarLogger.log.cannotStartResourceAdapter(key, throwable));
                    } else {
                        // the service is canceled rather than failed: the application still starts, and the
                        // services activating the endpoints of this resource adapter are canceled with it
                        QuarkusIronJacamarLogger.log.resourceAdapterNotStarted(key, throwable);
                        startContext.startCanceled();
                    }
                });
    }

    /**
     * Whether a resource adapter failing to start fails the startup of the application
     *
     * @param runtimeConfig The runtime configuration
     * @param key The resource adapter key
     * @return the value configured for this resource adapter, defaulting to the global one
     */
    public static boolean failOnStartError(IronJacamarRuntimeConfig runtimeConfig, String key) {
        IronJacamarRuntimeConfig.ResourceAdapterOuterNamedConfig config = runtimeConfig.resourceAdapters().get(key);
        if (config == null) {
            return runtimeConfig.failOnStartError();
        }
        return config.ra().failOnStartError().orElseGet(runtimeConfig::failOnStartError);
    }

    /**
     * Activate an endpoint
     * <p>
     * The service requires the start of the resource adapter, which only completes once it is started, so the
     * endpoint is activated synchronously and a failure fails the startup of the application, unless
     * <code>fail-on-start-error</code> is disabled for this resource adapter. When the resource adapter did not
     * start at all, this service is canceled along with the start it requires, and the endpoint is not activated.
     *
     * @param beanContainer The bean container
     * @param runtimeConfig The runtime configuration
     * @param resourceAdapterId The resource adapter id
     * @param activationSpecConfigId The activation spec config id
     * @param endpointClassName The endpoint class name
     * @param buildTimeConfig The build time config
     */
    public static void activateEndpoint(BeanContainer beanContainer,
            IronJacamarRuntimeConfig runtimeConfig,
            String resourceAdapterId,
            String activationSpecConfigId,
            String endpointClassName,
            Map<String, String> buildTimeConfig) {
        IronJacamarSupport ijSupport = beanContainer.beanInstance(IronJacamarSupport.class);
        try {
            ijSupport.activateEndpoint(resourceAdapterId, activationSpecConfigId, endpointClassName, buildTimeConfig);
        } catch (RuntimeException e) {
            if (failOnStartError(runtimeConfig, resourceAdapterId)) {
                throw e;
            }
            QuarkusIronJacamarLogger.log.endpointActivationFailed(endpointClassName, e);
        }
    }

    /**
     * Start the {@link IdleRemover} if any resource adapter pool requires it
     *
     * @param startContext The service start context
     * @param config The runtime configuration
     * @param executorService The Quarkus thread pool, which runs the idle remover task
     */
    public static void startIdleRemover(StartContext startContext, IronJacamarRuntimeConfig config,
            ScheduledExecutorService executorService) {
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
            IdleRemover instance = IdleRemover.getInstance();
            instance.setExecutorService(executorService);
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
     * @param config The runtime configuration
     * @param executorService The Quarkus thread pool, which runs the connection validator task
     */
    public static void startConnectionValidator(StartContext startContext, IronJacamarRuntimeConfig config,
            ScheduledExecutorService executorService) {
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
            ConnectionValidator instance = ConnectionValidator.getInstance();
            instance.setExecutorService(executorService);
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
