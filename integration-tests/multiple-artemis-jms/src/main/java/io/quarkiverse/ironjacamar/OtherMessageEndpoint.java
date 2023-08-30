package io.quarkiverse.ironjacamar;

import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.Queue;

import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Identifier;

@ResourceEndpoint(activationSpecConfigKey = "myqueue")
@Identifier("other")
public class OtherMessageEndpoint implements MessageListener {

    @Inject
    @Identifier(Defaults.DEFAULT_RESOURCE_ADAPTER_NAME)
    ConnectionFactory connectionFactory;

    @Override
    public void onMessage(Message message) {
        try {
            String body = message.getBody(String.class);
            Log.info("Received other message: " + body);
            try (JMSContext context = connectionFactory.createContext()) {
                Queue myQueue = context.createQueue("MyQueue");
                JMSProducer producer = context.createProducer();
                producer.send(myQueue, body);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
