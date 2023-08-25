package io.quarkiverse.ironjacamar.it;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class InjectionTest {

    @Test
    public void testProducer() {
        Assertions.assertThat(Arc.container().listAll(IronJacamarContainer.class)).hasSize(2);
    }

}
