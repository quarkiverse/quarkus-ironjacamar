package io.quarkiverse.jca.runtime.connection;

import java.util.Iterator;
import java.util.Set;

import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ManagedConnection;

import org.jboss.logging.Logger;

import io.vertx.core.impl.ConcurrentHashSet;

/**
 * A {@link ConnectionEventListener} that tracks connections and cleans them up on error or close.
 */
class CleanupConnectionListener implements ConnectionEventListener {

    private final Logger log = Logger.getLogger(CleanupConnectionListener.class);
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
