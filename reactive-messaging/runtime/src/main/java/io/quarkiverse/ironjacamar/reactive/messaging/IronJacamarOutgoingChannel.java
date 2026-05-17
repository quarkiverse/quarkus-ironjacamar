package io.quarkiverse.ironjacamar.reactive.messaging;

import java.util.Map;
import java.util.concurrent.Flow;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.providers.helpers.MultiUtils;

class IronJacamarOutgoingChannel {

    private final Flow.Subscriber<Message<?>> subscriber;

    IronJacamarOutgoingChannel(Object connectionFactory,
            ReactiveMessagingResourceAdapterSupport support,
            Map<String, String> outgoingConfig) {

        this.subscriber = MultiUtils.via(multi -> multi.call(message -> Uni.createFrom().completionStage(() -> {
            try {
                support.send(connectionFactory, message, outgoingConfig);
                return message.ack();
            } catch (Exception e) {
                return message.nack(e);
            }
        })));
    }

    Flow.Subscriber<Message<?>> getSubscriber() {
        return subscriber;
    }
}
