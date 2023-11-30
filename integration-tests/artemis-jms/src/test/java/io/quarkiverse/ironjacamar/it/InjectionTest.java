package io.quarkiverse.ironjacamar.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;

import org.junit.jupiter.api.Test;

import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkiverse.ironjacamar.runtime.TransactionRecoveryManager;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class InjectionTest {

    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    Instance<TransactionRecoveryManager> transactionRecoveryManagerInstance;

    @Test
    public void testProducer() {
        assertThat(Arc.container().listAll(IronJacamarContainer.class)).hasSize(1);
    }

    @Test
    public void shouldInjectConnectionFactory() {
        assertThat(Arc.container().listAll(ConnectionFactory.class)).hasSize(1);
        assertThat(connectionFactory).isNotNull();
    }

    @Test
    public void shouldInjectTransactionRecoveryManager() {
        assertSoftly(softly -> {
            softly.assertThat(Arc.container().listAll(TransactionRecoveryManager.class)).hasSize(1);
            softly.assertThat(transactionRecoveryManagerInstance).isNotNull();
            softly.assertThat(transactionRecoveryManagerInstance.isResolvable()).isTrue();
            softly.assertThat(transactionRecoveryManagerInstance.get()).isNotNull();
            softly.assertThat(transactionRecoveryManagerInstance.get().isEnabled()).isTrue();
        });
    }

}
