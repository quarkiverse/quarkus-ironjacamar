package io.quarkiverse.ironjacamar.reactive.messaging;

import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * SPI for bridging inbound JCA resource adapter messages into SmallRye Reactive Messaging.
 * <p>
 * Implementations tell the {@code ironjacamar} connector how to:
 * <ul>
 * <li>Determine the listener interface class the RA expects
 * (e.g., {@code jakarta.jms.MessageListener})</li>
 * <li>Create a listener instance that forwards RA-delivered payloads into the RM channel</li>
 * <li>Map channel configuration to activation spec configuration</li>
 * </ul>
 * Implementations must be CDI beans annotated with
 * {@link io.quarkiverse.ironjacamar.ResourceAdapterKind @ResourceAdapterKind}.
 */
public interface IncomingResourceAdapterSupport<T> {

    /**
     * The listener interface class that the resource adapter expects the endpoint to implement.
     * For JMS this would be {@code jakarta.jms.MessageListener.class}.
     *
     * @return the endpoint/listener class
     */
    Class<T> getEndpointClass();

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
    T createListener(Consumer<Message<?>> consumer);

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
}
