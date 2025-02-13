package io.quarkiverse.ironjacamar.runtime.endpoint;

import java.lang.reflect.Method;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.endpoint.MessageEndpoint;

/**
 * A {@link MessageEndpoint} that sets the context class loader to a specific class loader before calling the delegate
 */
class ClassLoaderMessageEndpoint extends MessageEndpointWrapper implements MessageEndpoint {
    private final ClassLoader classLoader;

    private ClassLoader originalClassLoader;

    public ClassLoaderMessageEndpoint(MessageEndpoint delegate, ClassLoader classLoader) {
        super(delegate);
        this.classLoader = classLoader;
    }

    @Override
    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {
        this.originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        super.beforeDelivery(method);
    }

    @Override
    public void afterDelivery() throws ResourceException {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
        super.afterDelivery();
    }

    @Override
    public void release() {
        this.originalClassLoader = null;
        super.release();
    }
}
