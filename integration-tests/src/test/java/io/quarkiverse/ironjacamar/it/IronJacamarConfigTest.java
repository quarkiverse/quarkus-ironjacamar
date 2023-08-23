package io.quarkiverse.ironjacamar.it;

import io.quarkiverse.ironjacamar.runtime.IronJacamarConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
                        assertThat(cp).isEqualTo("host=localhost;port=5445;protocols=HORNETQ");
                    });
                });

    }
}
