package io.quarkiverse.ironjacamar.reactive.it;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.logging.Log;

@ApplicationScoped
public class MessageConsumer {

    private final List<String> received = new CopyOnWriteArrayList<>();

    @Incoming("test-queue")
    public void consume(Message message) throws JMSException {
        String body = message.getBody(String.class);
        Log.infof("Received via Reactive Messaging: %s", body);
        received.add(body);
    }

    public List<String> getReceived() {
        return received;
    }

    public void clear() {
        received.clear();
    }
}
