package io.quarkiverse.jca.runtime.connection;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

import org.jboss.logging.Logger;

/**
 * A {@link ConnectionManager} provides a hook for the resource adapter to pass a connection request to Quarkus
 */
@ApplicationScoped
public class QuarkusConnectionManager implements ConnectionManager {

    private final Logger log = Logger.getLogger(QuarkusConnectionManager.class);

    private final CleanupConnectionListener cleanupConnectionListener = new CleanupConnectionListener();

    @Override
    public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        log.tracef("Allocating connection from %s", mcf);
        ManagedConnection mc = mcf.createManagedConnection(null, cxRequestInfo);
        cleanupConnectionListener.registerListener(mc);
        return mc.getConnection(null, cxRequestInfo);
    }

    @PreDestroy
    public void destroy() {
        cleanupConnectionListener.destroy();
    }

}
