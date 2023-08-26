package io.quarkiverse.ironjacamar.it;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkiverse.ironjacamar.runtime.IronJacamarConfig;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class IronJacamarConfigTest {

    @Inject
    IronJacamarConfig config;

    @Test
    void shouldReadConfig() {
        assertThat(config.enabled()).isTrue();
        assertThat(config.resourceAdapters().get("<default>").ra()).satisfies(
                ra -> {
                    assertThat(ra.kind()).contains("artemis");
                    assertThat(ra.config()).hasEntrySatisfying("connection-parameters", cp -> {
                        assertThat(cp).isEqualTo("host=localhost;port=61616;protocols=AMQP");
                    });
                });
        assertThat(config.activationSpecs().map().get("myqueue").config()).hasEntrySatisfying("destination", d -> {
            assertThat(d).isEqualTo("jms.queue.MyQueue");
        });

    }
}
