package io.quarkiverse.ironjacamar.test.start;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.ironjacamar.test.adapter.FailingResourceAdapter;
import io.quarkiverse.ironjacamar.test.adapter.FailingResourceAdapterFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestActivationSpec;
import io.quarkiverse.ironjacamar.test.adapter.TestConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestManagedConnectionFactory;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A resource adapter can require its own start, even though {@code fail-on-start-error} is globally disabled.
 */
public class ResourceAdapterOverridesFailOnStartErrorTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(
                            FailingResourceAdapterFactory.class,
                            FailingResourceAdapter.class,
                            TestManagedConnectionFactory.class,
                            TestConnectionFactory.class,
                            TestActivationSpec.class))
            .overrideConfigKey("quarkus.ironjacamar.ra.kind", "test")
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.idle-timeout-minutes", "0")
            .overrideConfigKey("quarkus.ironjacamar.fail-on-start-error", "false")
            .overrideConfigKey("quarkus.ironjacamar.ra.fail-on-start-error", "true")
            .assertException(throwable -> assertThat(StartFailures.messagesOf(throwable))
                    .contains(FailingResourceAdapter.START_FAILURE_MESSAGE));

    @Test
    void theResourceAdapterOverridesTheGlobalSetting() {
        // the application failing to start is the assertion
    }
}
