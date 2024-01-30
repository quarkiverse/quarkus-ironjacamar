package io.quarkiverse.ironjacamar.runtime;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Set;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

import org.jboss.jca.core.connectionmanager.TxConnectionManager;
import org.jboss.jca.core.spi.recovery.RecoveryPlugin;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.spi.transaction.XAResourceStatistics;
import org.jboss.jca.core.spi.transaction.recovery.XAResourceRecovery;

/**
 * Single entrypoint for transaction recovery management.
 */
public class TransactionRecoveryManager implements Closeable {

    private final TransactionIntegration transactionIntegration;

    private final RecoveryPlugin recoveryPlugin;

    private final Set<XAResourceRecovery> recoverySet = new HashSet<>();

    private final boolean enabled;

    /**
     * Constructor
     *
     * @param transactionIntegration The transaction integration
     * @param recoveryPlugin The recovery plugin
     * @param enabled Whether recovery is enabled
     */
    public TransactionRecoveryManager(TransactionIntegration transactionIntegration,
            RecoveryPlugin recoveryPlugin,
            boolean enabled) {
        this.transactionIntegration = transactionIntegration;
        this.recoveryPlugin = recoveryPlugin;
        this.enabled = enabled;
    }

    /**
     * Is recovery enabled?
     *
     * @return true if enabled; otherwise false
     */
    public boolean isEnabled() {
        return enabled;
    }

    /***
     * Register for recovery for inbound connections (e.g. JCA->Mainframe)
     *
     * @param mcf The managed connection factory
     * @param cm The connection manager
     * @param recoveryUsername The recovery username
     * @param recoveryPassword The recovery password
     * @param recoverySecurityDomain The recovery security domain
     * @throws ResourceException Thrown if an error occurs
     */
    public void registerForRecovery(ManagedConnectionFactory mcf, TxConnectionManager cm,
            String recoveryUsername, String recoveryPassword, String recoverySecurityDomain)
            throws ResourceException {
        XAResourceRecovery xaResourceRecovery = transactionIntegration.createXAResourceRecovery(mcf,
                cm.getPadXid(),
                cm.getIsSameRMOverride(),
                cm.getWrapXAResource(),
                recoveryUsername,
                recoveryPassword,
                recoverySecurityDomain,
                cm.getSubjectFactory(),
                recoveryPlugin,
                (XAResourceStatistics) cm.getPool().getStatistics());
        initialize(xaResourceRecovery);
    }

    /**
     * Register for recovery for outbound connections (e.g. Mainframe->JCA)
     *
     * @param resourceAdapter The resource adapter
     * @param activationSpec The activation spec
     * @param productName The product name
     * @param productVersion The product version
     * @throws ResourceException Thrown if an error occurs
     */
    public void registerForRecovery(ResourceAdapter resourceAdapter,
            ActivationSpec activationSpec, String productName, String productVersion) throws ResourceException {
        XAResourceRecovery xrr = transactionIntegration.createXAResourceRecovery(resourceAdapter, activationSpec, productName,
                productVersion);
        initialize(xrr);
    }

    /**
     * Initialize the recovery
     *
     * @param recovery The recovery
     * @throws ResourceException Thrown if an error occurs
     */
    private void initialize(XAResourceRecovery recovery) throws ResourceException {
        try {
            recovery.initialize();
            transactionIntegration.getRecoveryRegistry().addXAResourceRecovery(recovery);
            recoverySet.add(recovery);
        } catch (Exception e) {
            throw QuarkusIronJacamarLogger.log.errorDuringRecoveryInitialization(e);
        }
    }

    /**
     * Called when the application is shutting down
     */
    @Override
    public void close() {
        for (XAResourceRecovery xrr : recoverySet) {
            try {
                transactionIntegration.getRecoveryRegistry().removeXAResourceRecovery(xrr);
                xrr.shutdown();
            } catch (Exception e) {
                QuarkusIronJacamarLogger.log.errorDuringRecoveryShutdown(e);
            }
        }
        recoverySet.clear();
    }
}
