package io.quarkiverse.ironjacamar.runtime.endpoint;

import java.lang.reflect.Method;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.endpoint.MessageEndpoint;

import io.vertx.core.Context;

/**
 * A {@link MessageEndpointWrapper} implementation that duplicates the given {@link Context}
 * before invoking delivery methods on the wrapped {@link MessageEndpoint}.
 * <p>
 * This class ensures that a separate execution context is used for the delivery of messages,
 * which helps in isolating and managing the lifecycle of execution contexts.
 */
public class DuplicatedContextMessageEndpoint extends MessageEndpointWrapper {

    /**
     * Resolves {@code ContextInternal} from either the Vert.x 5.x package
     * ({@code io.vertx.core.internal}) or the Vert.x 4.x package
     * ({@code io.vertx.core.impl}), whichever is present on the classpath.
     */
    private static final Class<?> CONTEXT_INTERNAL_CLASS;
    private static final Method DUPLICATE_METHOD;
    private static final Method BEGIN_DISPATCH_METHOD;
    private static final Method END_DISPATCH_METHOD;
    private static final Method CURRENT_METHOD;

    static {
        Class<?> contextInternalClass;
        try {
            // Vert.x 5.x moved internal classes to io.vertx.core.internal
            contextInternalClass = Class.forName("io.vertx.core.internal.ContextInternal");
        } catch (ClassNotFoundException e) {
            try {
                // Vert.x 4.x internal classes are in io.vertx.core.impl
                contextInternalClass = Class.forName("io.vertx.core.impl.ContextInternal");
            } catch (ClassNotFoundException ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }
        CONTEXT_INTERNAL_CLASS = contextInternalClass;
        try {
            DUPLICATE_METHOD = contextInternalClass.getMethod("duplicate");
            BEGIN_DISPATCH_METHOD = contextInternalClass.getMethod("beginDispatch");
            END_DISPATCH_METHOD = contextInternalClass.getMethod("endDispatch", contextInternalClass);
            CURRENT_METHOD = contextInternalClass.getMethod("current");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

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
        try {
            Object duplicatedContext = DUPLICATE_METHOD.invoke(rootContext);
            BEGIN_DISPATCH_METHOD.invoke(duplicatedContext);
        } catch (ReflectiveOperationException e) {
            throw new ResourceException("Failed to setup Vert.x context for message delivery", e);
        }
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
            try {
                Object currentContext = CURRENT_METHOD.invoke(null);
                if (currentContext != null) {
                    END_DISPATCH_METHOD.invoke(currentContext, (Object) null);
                }
            } catch (ReflectiveOperationException e) {
                throw new ResourceException("Failed to cleanup Vert.x context after message delivery", e);
            }
        }
    }
}
