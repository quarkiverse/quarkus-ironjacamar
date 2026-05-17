package io.quarkiverse.ironjacamar.reactive.messaging;

import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * SPI for bridging a JCA resource adapter into SmallRye Reactive Messaging.
 * <p>
 * Extensions that wrap a specific resource adapter (e.g., Artemis JMS) implement
 * this interface to tell the generic {@code ironjacamar} connector how to:
 * <ul>
 * <li>Determine the listener interface class the RA expects
 * (e.g., {@code jakarta.jms.MessageListener})</li>
 * <li>Create a listener instance that forwards RA-delivered payloads into the RM channel</li>
 * <li>Map channel configuration to activation spec configuration</li>
 * </ul>
 * Implementations must be CDI beans annotated with
 * {@link io.quarkiverse.ironjacamar.ResourceAdapterKind @ResourceAdapterKind}.
 */
public interface ReactiveMessagingResourceAdapterSupport {

    /**
     * The listener interface class that the resource adapter expects the endpoint to implement.
     * For JMS this would be {@code jakarta.jms.MessageListener.class}.
     *
     * @return the endpoint/listener class
     */
    Class<?> getEndpointClass();

    /**
     * Create a listener instance that, when the RA delivers a message, converts it
     * to a {@link Message} and passes it to the given consumer.
     * <p>
     * The returned object must implement the interface returned by {@link #getEndpointClass()}.
     *
     * @param consumer the downstream consumer; each call to {@code accept()} pushes
     *        one message into the Reactive Messaging channel
     * @return the listener instance (e.g., a {@code MessageListener} for JMS)
     */
    Object createListener(Consumer<Message<?>> consumer);

    /**
     * Map the Reactive Messaging channel configuration into activation spec config entries
     * that the core IronJacamar extension understands.
     * <p>
     * The {@code channelConfig} contains all properties from
     * {@code mp.messaging.incoming.<channel>.*} with prefixes removed,
     * excluding reserved connector properties ({@code connector}, {@code resource-adapter-kind},
     * {@code resource-adapter-name}, {@code activation-spec-config-key}).
     *
     * @param channelConfig the raw channel configuration properties
     * @return the activation spec config map
     */
    Map<String, String> mapToActivationSpecConfig(Map<String, String> channelConfig);

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
