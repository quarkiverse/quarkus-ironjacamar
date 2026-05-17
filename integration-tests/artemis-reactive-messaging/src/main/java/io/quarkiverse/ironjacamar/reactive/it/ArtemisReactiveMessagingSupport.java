package io.quarkiverse.ironjacamar.reactive.it;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.BytesMessage;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSProducer;
import jakarta.jms.MessageListener;
import jakarta.jms.Queue;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.reactive.messaging.runtime.IncomingResourceAdapterSupport;
import io.quarkiverse.ironjacamar.reactive.messaging.runtime.OutgoingResourceAdapterSupport;
import io.vertx.core.json.Json;

@ApplicationScoped
@ResourceAdapterKind("artemis")
public class ArtemisReactiveMessagingSupport
        implements IncomingResourceAdapterSupport<MessageListener>, OutgoingResourceAdapterSupport {

    @Override
    public Class<MessageListener> getEndpointClass() {
        return MessageListener.class;
    }

    @Override
    public MessageListener createListener(Consumer<Object> consumer) {
        return consumer::accept;
    }

    @Override
    public Message<?> wrapMessage(Object rawMessage) {
        return Message.of(rawMessage);
    }

    @Override
    public Map<String, String> mapToActivationSpecConfig(Map<String, String> channelConfig) {
        Map<String, String> config = new HashMap<>();
        config.put("destination-type", channelConfig.getOrDefault("destination-type", "jakarta.jms.Queue"));
        config.put("destination", channelConfig.getOrDefault("destination", channelConfig.get("channel-name")));
        if (channelConfig.containsKey("max-session")) {
            config.put("max-session", channelConfig.get("max-session"));
        }
        if (channelConfig.containsKey("rebalance-connections")) {
            config.put("rebalance-connections", channelConfig.get("rebalance-connections"));
        }
        return config;
    }

    @Override
    public Map<String, String> mapToOutgoingConfig(Map<String, String> channelConfig) {
        Map<String, String> config = new HashMap<>();
        config.put("destination-type", channelConfig.getOrDefault("destination-type", "jakarta.jms.Queue"));
        config.put("destination", channelConfig.getOrDefault("destination", channelConfig.get("channel-name")));
        return config;
    }

    @Override
    public void send(Object connectionFactory, Message<?> message, Map<String, String> config) {
        Object payload = message.getPayload();
        try (JMSContext context = ((ConnectionFactory) connectionFactory).createContext()) {
            Queue queue = context.createQueue(config.get("destination"));
            JMSProducer producer = context.createProducer();

            if (payload instanceof jakarta.jms.Message) {
                producer.send(queue, (jakarta.jms.Message) payload);
            } else {
                jakarta.jms.Message outgoing;
                if (payload instanceof String || payload.getClass().isPrimitive() || isPrimitiveBoxed(payload.getClass())) {
                    outgoing = context.createTextMessage(payload.toString());
                } else if (payload instanceof byte[]) {
                    BytesMessage bytesMessage = context.createBytesMessage();
                    bytesMessage.writeBytes((byte[]) payload);
                    outgoing = bytesMessage;
                } else {
                    outgoing = context.createTextMessage(Json.encode(payload));
                }
                outgoing.setStringProperty("_classname", payload.getClass().getName());
                outgoing.setJMSType(payload.getClass().getName());
                producer.send(queue, outgoing);
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isPrimitiveBoxed(Class<?> c) {
        return c == Boolean.class || c == Integer.class || c == Byte.class
                || c == Double.class || c == Float.class || c == Short.class
                || c == Character.class || c == Long.class;
    }
}
