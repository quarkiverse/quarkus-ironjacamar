package io.quarkiverse.ironjacamar.runtime.endpoint;

import java.lang.reflect.Method;

import jakarta.resource.spi.endpoint.MessageEndpoint;

/**
 * A no-op message endpoint that does not support transactions.
 */
enum NoopMessageEndpoint implements MessageEndpoint {
    INSTANCE;

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeDelivery(Method method) {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterDelivery() {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() {
        // Do nothing
    }
}
