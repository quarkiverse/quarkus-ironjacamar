package io.quarkiverse.ironjacamar.runtime;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.jca.core.connectionmanager.pool.validator.ConnectionValidator;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

/**
 * Starts and stop the ConnectionValidator service
 */
@Dependent
public class ConnectionValidatorManager {

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    IronJacamarRuntimeConfig runtimeConfig;

    private static boolean shouldStartConnectionValidator;

    @PostConstruct
    void postConstruct() {
        for (IronJacamarRuntimeConfig.ResourceAdapterOuterNamedConfig value : runtimeConfig.resourceAdapters().values()) {
            if (value.ra().cm().pool().config().backgroundValidation()) {
                shouldStartConnectionValidator = true;
                break;
            }
        }
    }

    /**
     * Start the ConnectionValidator service
     */
    void startConnectionValidator(@Observes StartupEvent event) throws Throwable {
        if (!shouldStartConnectionValidator) {
            return;
        }
        QuarkusIronJacamarLogger.log.startConnectionValidatorService();
        // Start the ConnectionValidator service
        ConnectionValidator instance = ConnectionValidator.getInstance();
        instance.setExecutorService(managedExecutor);
        instance.start();
    }

    void stopConnectionValidator(@Observes ShutdownEvent event) throws Throwable {
        if (!shouldStartConnectionValidator) {
            return;
        }
        QuarkusIronJacamarLogger.log.stopConnectionValidatorService();
        // Stop the ConnectionValidator service
        ConnectionValidator.getInstance().stop();
    }
}
