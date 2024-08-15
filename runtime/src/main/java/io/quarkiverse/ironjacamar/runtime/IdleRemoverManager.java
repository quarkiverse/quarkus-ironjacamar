package io.quarkiverse.ironjacamar.runtime;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.jca.core.connectionmanager.pool.idle.IdleRemover;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

/**
 * Starts and stop the IdleRemover service
 */
@Dependent
public class IdleRemoverManager {

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    IronJacamarRuntimeConfig runtimeConfig;

    private static Boolean shouldStartIdleRemover;

    @PostConstruct
    void postConstruct() {
        if (shouldStartIdleRemover == null) {
            for (IronJacamarRuntimeConfig.ResourceAdapterOuterNamedConfig value : runtimeConfig.resourceAdapters().values()) {
                if (value.ra().cm().pool().config().idleTimeoutMinutes() > 0) {
                    shouldStartIdleRemover = Boolean.TRUE;
                    break;
                }
            }
        }
        if (shouldStartIdleRemover == null) {
            shouldStartIdleRemover = Boolean.FALSE;
        }
    }

    /**
     * Start the ConnectionValidator service
     */
    void startIdleRemover(@Observes StartupEvent event) throws Throwable {
        if (!shouldStartIdleRemover) {
            return;
        }
        QuarkusIronJacamarLogger.log.startIdleRemoverService();
        // Start the ConnectionValidator service
        IdleRemover instance = IdleRemover.getInstance();
        instance.setExecutorService(managedExecutor);
        instance.start();
    }

    void stopIdleRemover(@Observes ShutdownEvent event) throws Throwable {
        if (!shouldStartIdleRemover) {
            return;
        }
        QuarkusIronJacamarLogger.log.stopIdleRemoverService();
        // Stop the IdleRemover service
        IdleRemover.getInstance().stop();
    }
}
