package io.quarkiverse.ironjacamar.runtime.endpoint;

import java.lang.reflect.Method;
import java.util.Objects;

import javax.transaction.xa.XAResource;

import jakarta.resource.spi.UnavailableException;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

import io.quarkiverse.ironjacamar.Defaults;
import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.smallrye.common.annotation.Identifier;

public class DefaultMessageEndpointFactory implements MessageEndpointFactory {

    private final Class<?> endpointClass;
    private final String identifier;
    private final ResourceAdapterFactory resourceAdapterSupport;

    public DefaultMessageEndpointFactory(Class<?> endpointClass, String identifier, ResourceAdapterFactory adapterFactory) {
        this.endpointClass = endpointClass;
        this.identifier = identifier;
        this.resourceAdapterSupport = adapterFactory;
    }

    @Override
    public boolean isDeliveryTransacted(Method method) throws NoSuchMethodException {
        return TransactionAwareMessageEndpoint.isDeliveryTransacted(endpointClass, method);
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException {
        // TODO: Implement timeout
        return createEndpoint(xaResource);
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource) throws UnavailableException {
        Object endpointInstance = getEndpointInstance();
        MessageEndpoint endpoint = new TransactionAwareMessageEndpoint(xaResource, endpointClass);
        return resourceAdapterSupport.wrap(endpoint, endpointInstance);
    }

    @Override
    public String getActivationName() {
        return endpointClass.getName();
    }

    @Override
    public Class<?> getEndpointClass() {
        return endpointClass;
    }

    private Object getEndpointInstance() {
        Object instance;
        ArcContainer container = Arc.container();
        if (Defaults.DEFAULT_RESOURCE_ADAPTER_NAME.equals(identifier)) {
            // Try with default identifier and fallback to default if null
            instance = container.instance(endpointClass, Identifier.Literal.of(identifier)).get();
            if (instance == null) {
                instance = container.instance(endpointClass).get();
            }
        } else {
            instance = container.instance(endpointClass, Identifier.Literal.of(identifier)).get();
        }
        return Objects.requireNonNull(instance, "Unable to find endpoint instance for " + endpointClass.getName()
                + " with identifier " + identifier + " in Arc container");
    }

}
