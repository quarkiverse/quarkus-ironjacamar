package io.quarkiverse.jca.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

/**
 * A {@link ConnectionManager} provides a hook for the resource adapter to pass a connection request to Quarkus
 */
@ApplicationScoped
public class QuarkusConnectionManager implements ConnectionManager {

    @Override
    public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        ManagedConnection mc = mcf.createManagedConnection(null, cxRequestInfo);
        return mc.getConnection(null, cxRequestInfo);
    }
}
