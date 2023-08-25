package io.quarkiverse.ironjacamar.it;

import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class InjectionTest {

    @Inject
    //    @Identifier("other")
    ConnectionFactory connectionFactory;

    @Test
    public void testProducer() {
        Assertions.assertThat(Arc.container().listAll(IronJacamarContainer.class)).hasSize(2);
    }

    @Test
    public void shouldInjectConnectionFactory() {
        Assertions.assertThat(Arc.container().listAll(ConnectionFactory.class)).hasSize(1);
        System.out.println(connectionFactory);

    }

}
