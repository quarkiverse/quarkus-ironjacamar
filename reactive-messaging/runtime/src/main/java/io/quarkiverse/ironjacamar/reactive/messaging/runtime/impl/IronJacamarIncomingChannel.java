package io.quarkiverse.ironjacamar.reactive.messaging.runtime.impl;

import java.util.Map;
import java.util.concurrent.Flow;

import io.quarkiverse.ironjacamar.reactive.messaging.runtime.IncomingResourceAdapterSupport;
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

    private final UnicastProcessor<Message<?>> processor;
    private final Multi<Message<?>> stream;
    private final EndpointHandle endpointHandle;

    IronJacamarIncomingChannel(String resourceAdapterName,
            IronJacamarContainer container,
            IncomingResourceAdapterSupport<?> support,
            Map<String, String> activationSpecConfig) {

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
                    "Failed to activate JCA endpoint for resource adapter '" + resourceAdapterName + "'", e);
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
