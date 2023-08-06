package io.quarkiverse.jca.it.message;

import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import io.quarkiverse.jca.runtime.api.ActivationConfigProperty;
import io.quarkiverse.jca.runtime.api.MessageEndpoint;

@MessageEndpoint(activationConfig = {
        @ActivationConfigProperty(name = "destinationType", value = "jakarta.jms.Queue"),
        @ActivationConfigProperty(name = "maxSession", value = "3"),
        @ActivationConfigProperty(name = "destination", value = "MyQueue"),
        @ActivationConfigProperty(name = "rebalanceConnections", value = "true")
})
public class MyMessageEndpoint implements MessageListener {
    @Override
    public void onMessage(Message message) {
        System.out.println("Received message: " + message);
    }
}
