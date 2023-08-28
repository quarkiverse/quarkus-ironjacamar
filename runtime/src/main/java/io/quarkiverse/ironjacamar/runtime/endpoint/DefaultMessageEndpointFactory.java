package io.quarkiverse.ironjacamar.runtime.endpoint;

import java.lang.reflect.Method;

import javax.transaction.xa.XAResource;

import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.transaction.Transactional;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkus.arc.Arc;

public class DefaultMessageEndpointFactory implements MessageEndpointFactory {

    private final Class<?> endpointClass;
    private final ResourceAdapterFactory resourceAdapterSupport;

    public DefaultMessageEndpointFactory(Class<?> endpointClass, ResourceAdapterFactory adapterFactory) {
        this.endpointClass = endpointClass;
        this.resourceAdapterSupport = adapterFactory;
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource) {
        Object endpointInstance = getEndpointInstance();
        return resourceAdapterSupport.wrap(endpointInstance, new DefaultMessageEndpoint());
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) {
        Object endpointInstance = getEndpointInstance();
        return resourceAdapterSupport.wrap(endpointInstance, new DefaultMessageEndpoint());
    }

    @Override
    public boolean isDeliveryTransacted(Method method) {
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
