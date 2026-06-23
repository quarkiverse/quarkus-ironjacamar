package io.quarkiverse.ironjacamar.runtime.endpoint;

import java.lang.reflect.Method;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.endpoint.MessageEndpoint;

import io.vertx.core.Context;
import io.vertx.core.impl.ContextInternal;

/**
 * A {@link MessageEndpointWrapper} implementation that duplicates the given {@link Context}
 * before invoking delivery methods on the wrapped {@link MessageEndpoint}.
 * <p>
 * This class ensures that a separate execution context is used for the delivery of messages,
 * which helps in isolating and managing the lifecycle of execution contexts.
 */
public class DuplicatedContextMessageEndpoint extends MessageEndpointWrapper {

    private final Context rootContext;
    private ContextInternal duplicatedContext;
    private ContextInternal previousContext;

    /**
     * Constructor
     *
     * @param delegate The delegated message endpoint
     * @param rootContext Vert.x root context
     */
    public DuplicatedContextMessageEndpoint(MessageEndpoint delegate, Context rootContext) {
        super(delegate);
        this.rootContext = rootContext;
    }

    /**
     * Prepares the delivery of a message by duplicating the root context and beginning its dispatch.
     * <p>
     * The duplicated context is stored along with the previous thread-local context so that
     * {@link #afterDelivery()} can restore the original context after message processing completes.
     *
     * @param method The method that will handle the message delivery. This parameter represents the business
     *        method on the message endpoint class that is intended to receive the message.
     * @throws NoSuchMethodException If a method with the specified name and parameters does not exist within the
     *         message endpoint class.
     * @throws ResourceException If any error occurs during the preparation for message delivery.
     */
    @Override
    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {
        duplicatedContext = ((ContextInternal) rootContext).duplicate();
        previousContext = duplicatedContext.beginDispatch();
        super.beforeDelivery(method);
    }

    /**
     * Completes the delivery process by ending the dispatch on the duplicated context
     * and restoring the previous thread-local context that was active before
     * {@link #beforeDelivery(Method)} was called.
     *
     * @throws ResourceException if an error occurs while completing the delivery process
     */
    @Override
    public void afterDelivery() throws ResourceException {
        try {
            super.afterDelivery();
        } finally {
            try {
                duplicatedContext.endDispatch(previousContext);
            } finally {
                duplicatedContext = null;
                previousContext = null;
            }
        }
    }
}
