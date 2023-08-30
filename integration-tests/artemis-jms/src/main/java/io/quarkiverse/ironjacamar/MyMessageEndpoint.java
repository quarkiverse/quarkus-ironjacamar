package io.quarkiverse.ironjacamar;

import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import io.quarkus.logging.Log;

@ResourceEndpoint(activationSpecConfigKey = "myqueue")
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
