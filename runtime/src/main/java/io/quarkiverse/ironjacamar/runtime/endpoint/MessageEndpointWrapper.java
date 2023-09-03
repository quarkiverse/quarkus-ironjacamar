package io.quarkiverse.ironjacamar.runtime.endpoint;

import java.lang.reflect.Method;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.endpoint.MessageEndpoint;

public abstract class MessageEndpointWrapper implements MessageEndpoint {

    private final MessageEndpoint delegate;

    protected MessageEndpointWrapper(MessageEndpoint delegate) {
        this.delegate = delegate;
    }

    @Override
    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {
        delegate.beforeDelivery(method);
    }

    @Override
    public void afterDelivery() throws ResourceException {
        delegate.afterDelivery();
    }

    @Override
    public void release() {
        delegate.release();
    }
}
