package io.quarkiverse.ironjacamar.runtime;

import javax.naming.Reference;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl;
import org.jboss.jca.core.spi.recovery.RecoveryPlugin;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;

import io.quarkiverse.ironjacamar.runtime.security.QuarkusSecurityIntegration;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;

/**
 * {@link BeanCreator} implementations backing the synthetic beans registered by the deployment processor.
 */
public final class IronJacamarBeanCreators {

    private IronJacamarBeanCreators() {
    }

    /**
     * Creates the {@link IronJacamarContainer} synthetic bean for a given resource adapter.
     */
    public static class ContainerCreator implements BeanCreator<IronJacamarContainer> {

        @Override
        public IronJacamarContainer create(SyntheticCreationalContext<IronJacamarContainer> context) {
            String id = (String) context.getParams().get("id");
            String kind = (String) context.getParams().get("kind");
            IronJacamarSupport containerProducer = context.getInjectedReference(IronJacamarSupport.class);
            Vertx vertx = context.getInjectedReference(Vertx.class);
            return containerProducer.createContainer(vertx, id, kind);
        }
    }

    /**
     * Creates a connection factory from the given resource adapter (implementation vendor specific).
     */
    public static class ConnectionFactoryCreator implements BeanCreator<Object> {

        @Override
        public Object create(SyntheticCreationalContext<Object> context) {
            String id = (String) context.getParams().get("id");
            IronJacamarContainer container = context.getInjectedReference(IronJacamarContainer.class,
                    Identifier.Literal.of(id));
            try {
                Object connectionFactory = container.createConnectionFactory();
                if (connectionFactory instanceof Referenceable ref) {
                    // Set a reference to avoid serialization and JNDI lookups. See https://issues.apache.org/jira/browse/ARTEMIS-5253
                    ref.setReference(new Reference(connectionFactory.getClass().getCanonicalName(), null, null));
                }
                return connectionFactory;
            } catch (ResourceException e) {
                throw new DeploymentException("Cannot create connection factory", e);
            }
        }
    }

    /**
     * Creates the {@link CachedConnectionManager} synthetic bean.
     */
    public static class CachedConnectionManagerCreator implements BeanCreator<CachedConnectionManager> {

        @Override
        public CachedConnectionManager create(SyntheticCreationalContext<CachedConnectionManager> context) {
            TransactionIntegration ti = context.getInjectedReference(TransactionIntegration.class);
            CachedConnectionManagerImpl cachedConnectionManager = new CachedConnectionManagerImpl(ti);
            cachedConnectionManager.start();
            return cachedConnectionManager;
        }
    }

    /**
     * Creates the {@link QuarkusSecurityIntegration} synthetic bean.
     */
    public static class SecurityIntegrationCreator implements BeanCreator<QuarkusSecurityIntegration> {

        @Override
        public QuarkusSecurityIntegration create(SyntheticCreationalContext<QuarkusSecurityIntegration> context) {
            return new QuarkusSecurityIntegration();
        }
    }

    /**
     * Creates the {@link TransactionRecoveryManager} synthetic bean.
     */
    public static class TransactionRecoveryManagerCreator implements BeanCreator<TransactionRecoveryManager> {

        @Override
        public TransactionRecoveryManager create(SyntheticCreationalContext<TransactionRecoveryManager> context) {
            TransactionIntegration ti = context.getInjectedReference(TransactionIntegration.class);
            RecoveryPlugin recoveryPlugin = context.getInjectedReference(RecoveryPlugin.class);
            boolean enableRecovery = ConfigProvider.getConfig()
                    .getOptionalValue("quarkus.transaction-manager.enable-recovery", Boolean.class).orElse(false);
            return new TransactionRecoveryManager(ti, recoveryPlugin, enableRecovery);
        }
    }
}
