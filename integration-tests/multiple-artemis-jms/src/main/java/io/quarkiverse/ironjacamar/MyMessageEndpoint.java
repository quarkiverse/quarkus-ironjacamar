package io.quarkiverse.ironjacamar;

import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Identifier;

@ResourceEndpoint(activationSpecConfigKey = "myqueue")
@Identifier(Defaults.DEFAULT_RESOURCE_ADAPTER_NAME)
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
