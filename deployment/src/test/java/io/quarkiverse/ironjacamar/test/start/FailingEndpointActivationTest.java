package io.quarkiverse.ironjacamar.test.start;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.ironjacamar.test.adapter.FailingEndpointActivationResourceAdapterFactory;
import io.quarkiverse.ironjacamar.test.adapter.FailingResourceAdapter;
import io.quarkiverse.ironjacamar.test.adapter.TestActivationSpec;
import io.quarkiverse.ironjacamar.test.adapter.TestConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestManagedConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceEndpoint;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An endpoint that cannot be activated must fail the startup of the application: the activation used to be
 * registered as a callback on the deployment of the resource adapter verticle, so the failure was only reported
 * to the Vert.x uncaught exception handler while the application reported a successful startup.
 */
public class FailingEndpointActivationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(
                            FailingEndpointActivationResourceAdapterFactory.class,
                            FailingResourceAdapter.class,
                            TestManagedConnectionFactory.class,
                            TestConnectionFactory.class,
                            TestActivationSpec.class,
                            TestResourceEndpoint.class))
            .overrideConfigKey("quarkus.ironjacamar.ra.kind", "test")
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.idle-timeout-minutes", "0")
            .assertException(throwable -> assertThat(StartFailures.messagesOf(throwable))
                    .contains(FailingResourceAdapter.ACTIVATION_FAILURE_MESSAGE));

    @Test
    void startupFailsWhenAnEndpointCannotBeActivated() {
        // the application failing to start is the assertion
    }
}
