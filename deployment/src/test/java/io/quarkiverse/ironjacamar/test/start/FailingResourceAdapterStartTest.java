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
 * A resource adapter that fails to start must fail the startup of the application: the resource adapter is
 * deployed in a verticle, and the deployment used to be fire and forget, so the application reported a
 * successful startup while it had no working resource adapter.
 */
public class FailingResourceAdapterStartTest {

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
            .assertException(throwable -> assertThat(StartFailures.messagesOf(throwable))
                    .contains(FailingResourceAdapter.START_FAILURE_MESSAGE));

    @Test
    void startupFailsWhenTheResourceAdapterDoesNotStart() {
        // the application failing to start is the assertion
    }
}
