package io.quarkiverse.ironjacamar.it.jmslistener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.quarkiverse.ironjacamar.ResourceEndpoint;
import io.quarkiverse.jms.JmsListener;

@ResourceEndpoint
public class JmsListenerApp {

    private final List<String> received = new CopyOnWriteArrayList<>();

    @JmsListener("test-queue")
    void onMessage(String message) {
        received.add(message);
    }

    public List<String> getReceived() {
        return received;
    }
}
