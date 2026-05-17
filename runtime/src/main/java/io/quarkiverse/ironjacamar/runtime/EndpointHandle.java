package io.quarkiverse.ironjacamar.runtime;

import java.io.Closeable;

/**
 * Handle for a programmatically activated endpoint.
 * Call {@link #close()} to deactivate the endpoint with the resource adapter.
 */
@FunctionalInterface
public interface EndpointHandle extends Closeable {
    @Override
    void close();
}
