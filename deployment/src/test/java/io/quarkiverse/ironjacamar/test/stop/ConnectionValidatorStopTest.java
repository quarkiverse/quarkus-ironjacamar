package io.quarkiverse.ironjacamar.test.stop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * The ConnectionValidator is started when the background validation is enabled (the IdleRemover is disabled to
 * isolate it): the stop of the application must not wait for the Quarkus thread pool to interrupt its thread.
 */
public class ConnectionValidatorStopTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = StopDuration.application()
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.idle-timeout-minutes", "0")
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.background-validation", "true")
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.background-validation-millis", "60000");

    @Test
    void stopIsFastWithTheConnectionValidator() {
        // the stop duration is asserted once the application is stopped
    }
}
