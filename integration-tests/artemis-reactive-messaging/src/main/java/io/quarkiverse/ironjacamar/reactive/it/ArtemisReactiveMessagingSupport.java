package io.quarkiverse.ironjacamar.reactive.it;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.MessageListener;
import jakarta.jms.Queue;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.reactive.messaging.runtime.IncomingResourceAdapterSupport;
import io.quarkiverse.ironjacamar.reactive.messaging.runtime.OutgoingResourceAdapterSupport;

@ApplicationScoped
@ResourceAdapterKind("artemis")
public class ArtemisReactiveMessagingSupport
        implements IncomingResourceAdapterSupport<MessageListener>, OutgoingResourceAdapterSupport {

    @Override
    public Class<MessageListener> getEndpointClass() {
        return MessageListener.class;
    }

    @Override
    public MessageListener createListener(Consumer<Message<?>> consumer) {
        return message -> {
            try {
                String body = message.getBody(String.class);
                consumer.accept(Message.of(body));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
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
        try (JMSContext context = ((ConnectionFactory) connectionFactory).createContext()) {
            Queue queue = context.createQueue(config.get("destination"));
            JMSProducer producer = context.createProducer();
            producer.send(queue, (String) message.getPayload());
        }
    }
}
