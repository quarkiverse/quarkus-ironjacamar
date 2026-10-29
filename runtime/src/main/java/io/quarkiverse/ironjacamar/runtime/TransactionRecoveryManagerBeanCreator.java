package io.quarkiverse.ironjacamar.runtime;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jca.core.spi.recovery.RecoveryPlugin;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

/**
 * Creates the {@link TransactionRecoveryManager} synthetic bean.
 */
public class TransactionRecoveryManagerBeanCreator implements BeanCreator<TransactionRecoveryManager> {

    @Override
    public TransactionRecoveryManager create(SyntheticCreationalContext<TransactionRecoveryManager> context) {
        TransactionIntegration ti = context.getInjectedReference(TransactionIntegration.class);
        RecoveryPlugin recoveryPlugin = context.getInjectedReference(RecoveryPlugin.class);
        boolean enableRecovery = ConfigProvider.getConfig().getOptionalValue("quarkus.transaction-manager.enable-recovery",
                Boolean.class).orElse(false);
        return new TransactionRecoveryManager(ti, recoveryPlugin, enableRecovery);
    }
}
