package io.quarkiverse.ironjacamar.reactive.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class MessageProducer {

    @Inject
    @Channel("out-queue")
    Emitter<String> emitter;

    public void send(String text) {
        emitter.send(text);
    }
}
