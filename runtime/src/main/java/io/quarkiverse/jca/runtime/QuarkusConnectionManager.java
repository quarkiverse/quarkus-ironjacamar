package io.quarkiverse.jca.runtime;

import io.vertx.core.impl.ConcurrentHashSet;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import org.jboss.logging.Logger;

import java.util.Iterator;
import java.util.Set;

/**
 * A {@link ConnectionManager} provides a hook for the resource adapter to pass a connection request to Quarkus
 */
@ApplicationScoped
public class QuarkusConnectionManager implements ConnectionManager {

    private final Logger log = Logger.getLogger(QuarkusConnectionManager.class);

    private final ManagedConnectionTracker managedConnectionTracker = new ManagedConnectionTracker();

    @Override
    public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        ManagedConnection mc = mcf.createManagedConnection(null, cxRequestInfo);
        managedConnectionTracker.registerListener(mc);
        return mc.getConnection(null, cxRequestInfo);
    }

    @PreDestroy
    public void destroy() {
        managedConnectionTracker.destroy();
    }

    private class ManagedConnectionTracker implements ConnectionEventListener {

        private final Set<ManagedConnection> connections = new ConcurrentHashSet<>();

        @Override
        public void connectionClosed(ConnectionEvent event) {
            log.trace("Connection closed");
            connections.remove((ManagedConnection) event.getSource());
        }

        @Override
        public void localTransactionStarted(ConnectionEvent event) {

        }

        @Override
        public void localTransactionCommitted(ConnectionEvent event) {

        }

        @Override
        public void localTransactionRolledback(ConnectionEvent event) {

        }

        @Override
        public void connectionErrorOccurred(ConnectionEvent event) {
            log.trace("Connection error occurred", event.getException());
            ManagedConnection managedConnection = (ManagedConnection) event.getSource();
            connections.remove(managedConnection);
        }

        void registerListener(ManagedConnection connection) {
            connections.add(connection);
            connection.addConnectionEventListener(this);
        }

        void destroy() {
            log.tracef("Destroying connection listener with %s connections", connections.size());
            Iterator<ManagedConnection> iterator = connections.iterator();
            while (iterator.hasNext()) {
                ManagedConnection connection = iterator.next();
                try {
                    connection.destroy();
                } catch (Exception e) {
                    log.trace("Error while destroying managed connection", e);
                }
                iterator.remove();
            }
        }
    }
}
