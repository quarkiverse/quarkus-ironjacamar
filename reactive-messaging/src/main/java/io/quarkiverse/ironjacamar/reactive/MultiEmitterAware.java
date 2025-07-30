package io.quarkiverse.ironjacamar.reactive;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.mutiny.subscription.MultiEmitter;

/**
 *
 */
public interface MultiEmitterAware {
    void setMessageEmitter(MultiEmitter<? super Message<?>> emitter);
}
