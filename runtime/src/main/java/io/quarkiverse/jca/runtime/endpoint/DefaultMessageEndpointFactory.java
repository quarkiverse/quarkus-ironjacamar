package io.quarkiverse.jca.runtime.endpoint;

import java.lang.reflect.Method;

import javax.transaction.xa.XAResource;

import jakarta.resource.spi.UnavailableException;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.transaction.Transactional;

import io.quarkiverse.jca.spi.ResourceAdapterSupport;
import io.quarkus.arc.Arc;

public class DefaultMessageEndpointFactory implements MessageEndpointFactory {

    private final Class<?> endpointClass;
    private final ResourceAdapterSupport resourceAdapterSupport;

    public DefaultMessageEndpointFactory(Class<?> endpointClass, ResourceAdapterSupport resourceAdapterSupport) {
        this.endpointClass = endpointClass;
        this.resourceAdapterSupport = resourceAdapterSupport;
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource) throws UnavailableException {
        Object endpointInstance = getEndpointInstance();
        return resourceAdapterSupport.wrap(endpointInstance, new DefaultMessageEndpoint());
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException {
        Object endpointInstance = getEndpointInstance();
        return resourceAdapterSupport.wrap(endpointInstance, new DefaultMessageEndpoint());
    }

    @Override
    public boolean isDeliveryTransacted(Method method) throws NoSuchMethodException {
        return method.getAnnotation(Transactional.class) != null;
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
        return Arc.container().instance(endpointClass).get();
    }

}
