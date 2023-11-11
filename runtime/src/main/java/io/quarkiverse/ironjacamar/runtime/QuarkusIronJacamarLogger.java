package io.quarkiverse.ironjacamar.runtime;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

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

    QuarkusIronJacamarLogger log = Logger.getMessageLogger(QuarkusIronJacamarLogger.class,
            QuarkusIronJacamarLogger.class.getPackageName());

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Starting Resource Adapter %s: %s")
    void startingResourceAdapter(String id, String description);

    @LogMessage(level = INFO)
    @Message(id = 2, value = "Stopping Resource Adapter %s")
    void stoppingResourceAdapter(String id);

    @LogMessage(level = WARN)
    @Message(id = 3, value = "No default resource adapter kind found. IronJacamar is disabled")
    void noDefaultResourceAdapterKindFound();

    @Message(id = 4, value = "Resource adapter factory %s must be annotated with @ResourceAdapterKind")
    DeploymentException resourceAdapterFactoryMustBeAnnotatedException(String factory);

    @Message(id = 5, value = "Because there are more than one resource adapter configured, you need to explicitly use the @Identifier annotation on %s")
    DeploymentException useIdentifierAnnotation(String annotationTarget);

    @Message(id = 6, value = "Multiple kinds found (%s), please set the kind config for the %s configuration")
    ConfigurationException multipleKindsFound(Set<String> missing, String configuration);

    @LogMessage(level = WARN)
    @Message(id = 7, value = "The connection manager for the resource adapter %s is not transactional, therefore it cannot be registered for recovery")
    void connectionManagerNotTransactional(String adapter);

    @Message(id = 8, value = "Error during recovery initialization")
    ResourceException errorDuringRecoveryInitialization(@Cause Exception cause);

    @LogMessage(level = WARN)
    @Message(id = 9, value = "Error during recovery shutdown")
    void errorDuringRecoveryShutdown(@Cause Exception e);

    @Message(id = 10, value = "Cannot deploy resource adapter")
    DeploymentException cannotDeployResourceAdapter(@Cause Exception cause);

    @Message(id = 11, value = "Cannot activate endpoint")
    DeploymentException cannotActivateEndpoint(@Cause Exception cause);

}
