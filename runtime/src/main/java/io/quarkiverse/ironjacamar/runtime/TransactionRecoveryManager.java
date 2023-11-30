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

public class TransactionRecoveryManager implements Closeable {

    private final TransactionIntegration transactionIntegration;

    private final RecoveryPlugin recoveryPlugin;

    private final Set<XAResourceRecovery> recoverySet = new HashSet<>();

    private final boolean enabled;

    public TransactionRecoveryManager(TransactionIntegration transactionIntegration,
            RecoveryPlugin recoveryPlugin,
            boolean enabled) {
        this.transactionIntegration = transactionIntegration;
        this.recoveryPlugin = recoveryPlugin;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

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

    public void registerForRecovery(ResourceAdapter resourceAdapter,
            ActivationSpec activationSpec, String productName, String productVersion) throws ResourceException {
        XAResourceRecovery xrr = transactionIntegration.createXAResourceRecovery(resourceAdapter, activationSpec, productName,
                productVersion);
        initialize(xrr);
    }

    void initialize(XAResourceRecovery recovery) throws ResourceException {
        try {
            recovery.initialize();
            transactionIntegration.getRecoveryRegistry().addXAResourceRecovery(recovery);
            recoverySet.add(recovery);
        } catch (Exception e) {
            throw QuarkusIronJacamarLogger.log.errorDuringRecoveryInitialization(e);
        }
    }

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
