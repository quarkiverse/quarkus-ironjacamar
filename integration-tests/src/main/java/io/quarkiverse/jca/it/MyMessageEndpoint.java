package io.quarkiverse.jca.it;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import io.quarkiverse.jca.runtime.api.ActivationConfigProperty;
import io.quarkiverse.jca.runtime.api.ResourceEndpoint;
import io.quarkus.logging.Log;

@ResourceEndpoint(activationConfig = {
        @ActivationConfigProperty(name = "destinationType", value = "jakarta.jms.Queue"),
        @ActivationConfigProperty(name = "maxSession", value = "3"),
        @ActivationConfigProperty(name = "destination", value = "MyQueue"),
        @ActivationConfigProperty(name = "rebalanceConnections", value = "true")
})
public class MyMessageEndpoint implements MessageListener {
    @Override
    public void onMessage(Message message) {
        try {
            Log.info("Received message: " + message.getBody(String.class));
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
