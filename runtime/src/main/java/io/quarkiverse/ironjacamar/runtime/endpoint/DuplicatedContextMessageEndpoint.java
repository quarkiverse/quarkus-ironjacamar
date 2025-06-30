package io.quarkiverse.ironjacamar.runtime.endpoint;

import java.lang.reflect.Method;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.endpoint.MessageEndpoint;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
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
     * Prepares the delivery of a message by duplicating the execution context and beginning its dispatch.
     *
     * @param method The method that will handle the message delivery. This parameter represents the business
     *        method on the message endpoint class that is intended to receive the message.
     * @throws NoSuchMethodException If a method with the specified name and parameters does not exist within the
     *         message endpoint class.
     * @throws ResourceException If any error occurs during the preparation for message delivery.
     */
    @Override
    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {
        ((ContextInternal) rootContext).duplicate().beginDispatch();
        super.beforeDelivery(method);
    }

    /**
     * Completes the delivery process by performing necessary actions in the current execution context.
     * <p>
     * This method is invoked after a message has been delivered to the message endpoint. It ensures
     * the proper management of the execution context by calling the superclass implementation
     * to trigger any delegate-specific actions and by signaling the end of the dispatch in the
     * current Vert.x context. This is critical to clean up and conclude the message processing cycle
     * for the current context.
     *
     * @throws ResourceException if an error occurs while completing the delivery process
     */
    @Override
    public void afterDelivery() throws ResourceException {
        try {
            super.afterDelivery();
        } finally {
            ContextInternal currentContext = (ContextInternal) Vertx.currentContext();
            if (currentContext != null) {
                currentContext.endDispatch(null);
            }
        }
    }
}
