package io.quarkiverse.ironjacamar.it;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkiverse.ironjacamar.runtime.IronJacamarRuntimeConfig;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class IronJacamarConfigTest {

    @Inject
    IronJacamarRuntimeConfig runtimeConfig;

    @Test
    void shouldReadConfig() {
        assertThat(runtimeConfig.resourceAdapters().get("<default>").ra()).satisfies(
                ra -> {
                    assertThat(ra.config()).hasEntrySatisfying("connection-parameters", cp -> {
                        assertThat(cp).isEqualTo("host=localhost;port=61616;protocols=AMQP");
                    });
                });
        assertThat(runtimeConfig.activationSpecs().map().get("myqueue").config()).hasEntrySatisfying("destination", d -> {
            assertThat(d).isEqualTo("jms.queue.MyQueue");
        });

    }
}
