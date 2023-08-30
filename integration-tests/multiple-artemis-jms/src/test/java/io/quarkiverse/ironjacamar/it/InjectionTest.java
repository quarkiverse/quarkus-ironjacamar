package io.quarkiverse.ironjacamar.it;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;

import org.junit.jupiter.api.Test;

import io.quarkiverse.ironjacamar.Defaults;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.common.annotation.Identifier;

@QuarkusTest
public class InjectionTest {

    @Inject
    @Identifier(Defaults.DEFAULT_RESOURCE_ADAPTER_NAME)
    ConnectionFactory connectionFactory;

    @Inject
    @Identifier("other")
    ConnectionFactory otherConnectionFactory;

    @Test
    public void testProducer() {
        assertThat(Arc.container().listAll(IronJacamarContainer.class)).hasSize(2);
    }

    @Test
    public void shouldInjectConnectionFactory() {
        assertThat(Arc.container().listAll(ConnectionFactory.class)).hasSize(2);
        assertThat(connectionFactory).isNotNull();
        assertThat(otherConnectionFactory).isNotNull();
    }

}
