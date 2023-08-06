package io.quarkiverse.jca.runtime.impl;

import java.lang.reflect.Method;

import javax.transaction.xa.XAResource;

import jakarta.resource.spi.UnavailableException;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.transaction.Transactional;

public class DefaultMessageEndpointFactory implements MessageEndpointFactory {
    private final Class<?> endpointClass;

    public DefaultMessageEndpointFactory(Class<?> endpointClass) {
        this.endpointClass = endpointClass;
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource) throws UnavailableException {
        return new DefaultMessageEndpoint();
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException {
        return new DefaultMessageEndpoint();
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
}
