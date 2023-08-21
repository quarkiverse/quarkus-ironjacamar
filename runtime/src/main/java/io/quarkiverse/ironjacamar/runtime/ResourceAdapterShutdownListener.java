package io.quarkiverse.ironjacamar.runtime;

import java.util.Objects;
import java.util.Set;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

import io.quarkus.runtime.shutdown.ShutdownListener;
import io.vertx.core.impl.ConcurrentHashSet;

/**
 * Invokes {@link ResourceAdapter#endpointDeactivation(MessageEndpointFactory, ActivationSpec)} on shutdown.
 */
public class ResourceAdapterShutdownListener implements ShutdownListener {

    private final ResourceAdapter resourceAdapter;
    private final Set<EndpointRegistration> registrations = new ConcurrentHashSet<>();

    public ResourceAdapterShutdownListener(ResourceAdapter adapter) {
        this.resourceAdapter = adapter;
    }

    public void registerEndpoint(MessageEndpointFactory endpointFactory,
            ActivationSpec spec) {
        registrations.add(new EndpointRegistration(endpointFactory, spec));
    }

    @Override
    public void preShutdown(ShutdownNotification notification) {
        for (EndpointRegistration registration : registrations) {
            resourceAdapter.endpointDeactivation(registration.messageEndpointFactory,
                    registration.activationSpec);
        }
        notification.done();
    }

    private static class EndpointRegistration {

        final MessageEndpointFactory messageEndpointFactory;
        final ActivationSpec activationSpec;

        public EndpointRegistration(MessageEndpointFactory messageEndpointFactory,
                ActivationSpec activationSpec) {
            this.messageEndpointFactory = messageEndpointFactory;
            this.activationSpec = activationSpec;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            EndpointRegistration that = (EndpointRegistration) o;
            return Objects.equals(messageEndpointFactory, that.messageEndpointFactory)
                    && Objects.equals(activationSpec, that.activationSpec);
        }

        @Override
        public int hashCode() {
            return Objects.hash(messageEndpointFactory, activationSpec);
        }
    }
}
