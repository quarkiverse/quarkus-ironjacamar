package io.quarkiverse.ironjacamar.reactive.messaging;

import java.util.Map;
import java.util.concurrent.Flow;

import jakarta.resource.ResourceException;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkiverse.ironjacamar.runtime.EndpointHandle;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;

/**
 * Manages the lifecycle of a single incoming Reactive Messaging channel backed by a JCA endpoint.
 */
class IronJacamarIncomingChannel {

    private final String channelName;
    private final UnicastProcessor<Message<?>> processor;
    private final Multi<Message<?>> stream;
    private final EndpointHandle endpointHandle;

    IronJacamarIncomingChannel(String channelName, String resourceAdapterKind, String resourceAdapterName,
            IronJacamarContainer container,
            ReactiveMessagingResourceAdapterSupport support,
            Map<String, String> activationSpecConfig) {

        this.channelName = channelName;
        this.processor = UnicastProcessor.create();

        Object listener = support.createListener(processor::onNext);

        try {
            this.endpointHandle = container.endpointActivation(
                    support.getEndpointClass(),
                    resourceAdapterName,
                    activationSpecConfig,
                    listener);
        } catch (ResourceException e) {
            throw new IllegalStateException(
                    "Failed to activate JCA endpoint for channel '" + channelName + "'", e);
        }

        this.stream = processor
                .onTermination().invoke(this::close);
    }

    Flow.Publisher<Message<?>> getPublisher() {
        return stream;
    }

    void close() {
        endpointHandle.close();
    }
}
