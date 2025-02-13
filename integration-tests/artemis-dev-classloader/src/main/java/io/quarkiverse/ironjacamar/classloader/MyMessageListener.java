package io.quarkiverse.ironjacamar.classloader;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.Queue;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkiverse.ironjacamar.ResourceEndpoint;

@ResourceEndpoint(activationSpecConfigKey = "qa")
public class MyMessageListener implements MessageListener {

    @Inject
    @RestClient
    Instance<MyRestClient> myRestClient;

    @Inject
    ConnectionFactory connectionFactory;

    @Override
    public void onMessage(Message message) {
        try (JMSContext context = connectionFactory.createContext()) {
            JMSProducer producer = context.createProducer();
            Queue queue = context.createQueue("reply");
            //            producer.send(queue, myRestClient.get().hello());
            producer.send(queue, "Hello, World!");
        }
    }
}
