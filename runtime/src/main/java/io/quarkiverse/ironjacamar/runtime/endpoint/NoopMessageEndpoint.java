package io.quarkiverse.ironjacamar.runtime.endpoint;

import java.lang.reflect.Method;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.endpoint.MessageEndpoint;

/**
 * A no-op message endpoint that does not support transactions.
 */
enum NoopMessageEndpoint implements MessageEndpoint {
    INSTANCE;

    @Override
    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {
        // Do nothing
    }

    @Override
    public void afterDelivery() throws ResourceException {
        // Do nothing
    }

    @Override
    public void release() {
        // Do nothing
    }
}
