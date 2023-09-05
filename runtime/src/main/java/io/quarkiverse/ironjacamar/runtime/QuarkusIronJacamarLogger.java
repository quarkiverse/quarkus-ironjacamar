package io.quarkiverse.ironjacamar.runtime;

import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

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

}
