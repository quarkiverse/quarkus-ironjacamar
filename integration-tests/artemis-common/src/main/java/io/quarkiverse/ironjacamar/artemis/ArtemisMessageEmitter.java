package io.quarkiverse.ironjacamar.artemis;

import jakarta.jms.MessageListener;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkiverse.ironjacamar.reactive.MultiEmitterAware;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class ArtemisMessageEmitter implements MultiEmitterAware, MessageListener {

    private MultiEmitter<? super Message<?>> emitter;

    @Override
    public void setMessageEmitter(MultiEmitter<? super Message<?>> emitter) {
        this.emitter = emitter;
    }

    @Override
    public void onMessage(jakarta.jms.Message message) {
        if (!emitter.isCancelled()) {
            emitter.emit(Message.of(message));
        } else {
            throw new IllegalStateException("Message emitter has been cancelled");
        }
    }
}
