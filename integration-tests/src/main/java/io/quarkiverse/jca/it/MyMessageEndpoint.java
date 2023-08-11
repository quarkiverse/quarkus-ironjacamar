package io.quarkiverse.jca.it;

import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import io.quarkiverse.jca.runtime.endpoint.ResourceEndpoint;
import io.quarkus.logging.Log;

@ResourceEndpoint
public class MyMessageEndpoint implements MessageListener {
    @Override
    public void onMessage(Message message) {
        try {
            Log.info("Received message: " + message.getBody(String.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}