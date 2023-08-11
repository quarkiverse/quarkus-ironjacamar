package io.quarkiverse.jca.runtime;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

import io.quarkus.logging.Log;

/**
 * A {@link ConnectionManager} provides a hook for the resource adapter to pass a connection request to Quarkus
 */
@ApplicationScoped
public class QuarkusConnectionManager implements ConnectionManager {

    private final ConcurrentHashMap<ManagedConnection, Long> connections = new ConcurrentHashMap<>();

    @Override
    public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        ManagedConnection mc = mcf.createManagedConnection(null, cxRequestInfo);
        connections.put(mc, System.currentTimeMillis());
        return mc.getConnection(null, cxRequestInfo);
    }

    @PreDestroy
    public void destroy() {
        Iterator<ManagedConnection> iterator = connections.keySet().iterator();
        while (iterator.hasNext()) {
            ManagedConnection connection = iterator.next();
            try {
                connection.destroy();
            } catch (Exception e) {
                Log.trace("Error while destroying managed connection", e);
            }
            iterator.remove();
        }
    }

}
