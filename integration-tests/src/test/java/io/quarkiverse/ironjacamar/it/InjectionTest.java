package io.quarkiverse.ironjacamar.it;

import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.common.annotation.Identifier;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.ra.ActiveMQRAConnectionFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class InjectionTest {

//    @Inject
//    @Identifier("other")
    ConnectionFactory connectionFactory;

    @Test
    public void testProducer() {
        Assertions.assertThat(Arc.container().listAll(IronJacamarContainer.class)).hasSize(2);
    }

    @Test
    public void shouldInjectConnectionFactory() {
        Assertions.assertThat(Arc.container().listAll(ActiveMQRAConnectionFactory.class)).hasSize(2);
        System.out.println(connectionFactory);

    }

}
