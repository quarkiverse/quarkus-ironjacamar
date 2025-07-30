package io.quarkiverse.ironjacamar.reactive;

import java.util.Map;
import java.util.concurrent.Flow;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;

import io.quarkiverse.ironjacamar.runtime.IronJacamarSupport;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.connector.InboundConnector;

@ApplicationScoped
@Connector(IronJacamarInboundConnector.CONNECTOR_NAME)
public class IronJacamarInboundConnector implements InboundConnector {

    /**
     * The name of the connector: {@code ironjacamar}
     */
    public static final String CONNECTOR_NAME = "ironjacamar";

    @Inject
    IronJacamarSupport ironJacamarSupport;

    @Override
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    public Flow.Publisher<? extends Message<?>> getPublisher(Config config) {
        // Channel name must match the activation spec ID
        String channelName = config.getValue(CHANNEL_NAME_ATTRIBUTE, String.class);
        return Multi.createFrom().emitter((m) -> {
            // Create endpoint instance
            // TODO: Find the endpoint instance
            MultiEmitterAware endpointInstance = message -> {
            };
            endpointInstance.setMessageEmitter(m);
            ironJacamarSupport.activateEndpoint("UNKNOWN", channelName, endpointInstance, Map.of());
        });
    }

}
