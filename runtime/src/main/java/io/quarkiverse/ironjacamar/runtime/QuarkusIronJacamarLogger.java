package io.quarkiverse.ironjacamar.runtime;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.util.Set;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
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

}
