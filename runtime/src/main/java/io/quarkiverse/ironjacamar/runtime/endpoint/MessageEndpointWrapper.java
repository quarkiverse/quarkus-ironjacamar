package io.quarkiverse.ironjacamar.runtime.endpoint;

import java.lang.reflect.Method;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.endpoint.MessageEndpoint;

/**
 * A message endpoint wrapper. Use this class when you want to wrap an existing message endpoint.
 */
public abstract class MessageEndpointWrapper implements MessageEndpoint {

    protected final MessageEndpoint delegate;

    /**
     * Constructor
     *
     * @param delegate The delegated message endpoint
     */
    protected MessageEndpointWrapper(MessageEndpoint delegate) {
        this.delegate = delegate;
    }

    /**
     * Calls the delegate {@link MessageEndpoint#beforeDelivery(Method)} method.
     */
    @Override
    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {
        delegate.beforeDelivery(method);
    }

    /**
     * Calls the delegate {@link MessageEndpoint#afterDelivery()} method.
     */
    @Override
    public void afterDelivery() throws ResourceException {
        delegate.afterDelivery();
    }

    /**
     * Calls the delegate {@link MessageEndpoint#release()} method.
     */
    @Override
    public void release() {
        delegate.release();
    }
}
