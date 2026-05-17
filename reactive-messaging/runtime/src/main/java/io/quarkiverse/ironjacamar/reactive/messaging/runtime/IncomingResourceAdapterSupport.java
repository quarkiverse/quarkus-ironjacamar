package io.quarkiverse.ironjacamar.reactive.messaging.runtime;

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
     * Create a listener instance that forwards raw transport messages to the given consumer.
     * <p>
     * The returned object must implement the interface returned by {@link #getEndpointClass()}.
     * The consumer receives the raw transport-specific message object (e.g., {@code jakarta.jms.Message}).
     * The connector will then call {@link #wrapMessage(Object)} to convert it into a
     * Reactive Messaging {@link Message}.
     *
     * @param consumer the downstream consumer; each call to {@code accept()} pushes
     *        one raw transport message for wrapping
     * @return the listener instance (e.g., a {@code MessageListener} for JMS)
     */
    T createListener(Consumer<Object> consumer);

    /**
     * Wrap a raw transport message into a Reactive Messaging {@link Message}.
     * <p>
     * Implementations control how the transport-specific payload is exposed to
     * {@code @Incoming} consumers. For JMS, this would typically wrap the
     * {@code jakarta.jms.Message} as the payload.
     *
     * @param rawMessage the raw transport message received by the listener
     * @return a Reactive Messaging message wrapping the transport payload
     */
    default Message<?> wrapMessage(Object rawMessage) {
        return Message.of(rawMessage);
    }

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
