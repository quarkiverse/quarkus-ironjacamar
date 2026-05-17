package io.quarkiverse.ironjacamar.reactive.messaging.runtime;

import java.util.Map;

import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * SPI for sending messages through a JCA resource adapter via SmallRye Reactive Messaging.
 * <p>
 * Implementations tell the {@code ironjacamar} connector how to:
 * <ul>
 * <li>Map outgoing channel configuration to transport-specific properties</li>
 * <li>Send a message using the JCA-managed connection factory</li>
 * </ul>
 * Implementations must be CDI beans annotated with
 * {@link io.quarkiverse.ironjacamar.ResourceAdapterKind @ResourceAdapterKind}.
 */
public interface OutgoingResourceAdapterSupport {

    /**
     * Map outgoing channel configuration to transport-specific properties.
     * <p>
     * The {@code channelConfig} contains all properties from
     * {@code mp.messaging.outgoing.<channel>.*} with prefixes removed,
     * excluding reserved connector properties.
     *
     * @param channelConfig the raw channel configuration properties
     * @return the outgoing config map
     */
    Map<String, String> mapToOutgoingConfig(Map<String, String> channelConfig);

    /**
     * Send a message using the given connection factory.
     * <p>
     * Implementations should create a connection/session from the factory, send the message payload,
     * and clean up resources. For JMS this would create a {@code JMSContext}, produce to the
     * configured destination, and close the context.
     *
     * @param connectionFactory the JCA-managed connection factory (e.g., {@code jakarta.jms.ConnectionFactory})
     * @param message the Reactive Messaging message to send
     * @param config the outgoing channel configuration (as returned by {@link #mapToOutgoingConfig})
     */
    void send(Object connectionFactory, Message<?> message, Map<String, String> config);
}
