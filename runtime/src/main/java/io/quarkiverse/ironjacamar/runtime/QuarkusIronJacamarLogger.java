package io.quarkiverse.ironjacamar.runtime;

import static org.jboss.logging.Logger.Level.*;

import java.util.Set;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.resource.ResourceException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * The Quarkus IronJacamar logger.
 * <p>
 * Message ids ranging from 000000 to 009999 inclusively.
 */
@MessageLogger(projectCode = "QIJ")
public interface QuarkusIronJacamarLogger extends BasicLogger {

    /**
     * A logger with the category {@code io.quarkiverse.ironjacamar.runtime}.
     */
    QuarkusIronJacamarLogger log = Logger.getMessageLogger(QuarkusIronJacamarLogger.class,
            QuarkusIronJacamarLogger.class.getPackageName());

    /**
     * Logs a message indicating that the resource adapter is starting.
     *
     * @param id the resource adapter id
     * @param description the resource adapter description
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Starting Resource Adapter %s: %s")
    void startingResourceAdapter(String id, String description);

    /**
     * Logs a message indicating that the resource adapter is stopping.
     *
     * @param id the resource adapter id
     */
    @LogMessage(level = INFO)
    @Message(id = 2, value = "Stopping Resource Adapter %s")
    void stoppingResourceAdapter(String id);

    /**
     * Logs a message indicating that no default resource adapter kind was found.
     */
    @LogMessage(level = WARN)
    @Message(id = 3, value = "No default resource adapter kind found. IronJacamar is disabled")
    void noDefaultResourceAdapterKindFound();

    /**
     * Returns a {@link DeploymentException} indicating that the resource adapter factory must be annotated with
     * {@link io.quarkiverse.ironjacamar.ResourceAdapterKind}.
     *
     * @param factory the resource adapter factory
     * @return the exception
     */
    @Message(id = 4, value = "Resource adapter factory %s must be annotated with @ResourceAdapterKind")
    DeploymentException resourceAdapterFactoryMustBeAnnotatedException(String factory);

    /**
     * Returns a {@link DeploymentException} indicating that the {@link io.smallrye.common.annotation.Identifier} annotation
     * must be used.
     *
     * @param annotationTarget the annotation target
     * @return the exception
     */
    @Message(id = 5, value = "Because there are more than one resource adapter configured, you need to explicitly use the @Identifier annotation on %s")
    DeploymentException useIdentifierAnnotation(String annotationTarget);

    /**
     * Returns a {@link ConfigurationException} indicating that multiple kinds were found.
     *
     * @param missing the missing kinds
     * @param configuration the configuration
     * @return the exception
     */
    @Message(id = 6, value = "Multiple kinds found (%s), please set the kind config for the %s configuration")
    ConfigurationException multipleKindsFound(Set<String> missing, String configuration);

    /**
     * Logs a message indicating that the connection manager is not transactional.
     *
     * @param adapter the resource adapter
     */
    @LogMessage(level = WARN)
    @Message(id = 7, value = "The connection manager for the resource adapter %s is not transactional, therefore it cannot be registered for recovery")
    void connectionManagerNotTransactional(String adapter);

    /**
     * Returns a {@link ResourceException} indicating that an error occurred during recovery initialization.
     *
     * @param cause the cause
     * @return the exception
     */
    @Message(id = 8, value = "Error during recovery initialization")
    ResourceException errorDuringRecoveryInitialization(@Cause Exception cause);

    /**
     * Logs a message indicating that an error occurred during recovery shutdown.
     *
     * @param e the exception
     */
    @LogMessage(level = WARN)
    @Message(id = 9, value = "Error during recovery shutdown")
    void errorDuringRecoveryShutdown(@Cause Exception e);

    /**
     * Returns a {@link DeploymentException} indicating that the resource adapter cannot be deployed.
     *
     * @param cause the cause
     * @return the exception
     */
    @Message(id = 10, value = "Cannot deploy resource adapter")
    DeploymentException cannotDeployResourceAdapter(@Cause Exception cause);

    /**
     * Returns a {@link DeploymentException} indicating that the endpoint cannot be activated.
     *
     * @param cause the cause
     * @return the exception
     */
    @Message(id = 11, value = "Cannot activate endpoint")
    DeploymentException cannotActivateEndpoint(@Cause Exception cause);

    /**
     * Logs a message indicating that the resource adapter factory types are not defined.
     *
     * @param type the resource adapter factory type
     */
    @LogMessage(level = WARN)
    @Message(id = 12, value = "The @ResourceAdapterTypes annotation was not found in %s. Injection of ConnectionFactories for this ResourceAdapter will not work")
    void resourceAdapterTypesNotDefined(String type);

    /**
     * Logs a message indicating that the connection validator service is starting.
     */
    @LogMessage(level = INFO)
    @Message(id = 13, value = "Starting Connection Validator service")
    void startConnectionValidatorService();

    /**
     * Logs a message indicating that the connection validator service is stopping.
     */
    @LogMessage(level = INFO)
    @Message(id = 14, value = "Stopping Connection Validator service")
    void stopConnectionValidatorService();

    /**
     * Logs a message indicating that the connection validator service is starting.
     */
    @LogMessage(level = INFO)
    @Message(id = 15, value = "Starting Idle Remover service")
    void startIdleRemoverService();

    /**
     * Logs a message indicating that the connection validator service is stopping.
     */
    @LogMessage(level = INFO)
    @Message(id = 16, value = "Stopping Idle Remover service")
    void stopIdleRemoverService();

}
