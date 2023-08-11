package io.quarkiverse.jca.runtime;

import io.quarkus.logging.Log;
import io.vertx.core.impl.ConcurrentHashSet;
import jakarta.annotation.PreDestroy;
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

    ConcurrentHashSet<ManagedConnection> connections = new ConcurrentHashSet<>();

    @Override
    public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        ManagedConnection mc = mcf.createManagedConnection(null, cxRequestInfo);
        connections.add(mc);
        return mc.getConnection(null, cxRequestInfo);
    }


    @PreDestroy
    public void destroy() {
        for (ManagedConnection connection : connections) {
            try {
                connection.destroy();
            } catch (Exception e) {
                Log.trace("Error while destroying managed connection", e);
            }
        }
    }

}
