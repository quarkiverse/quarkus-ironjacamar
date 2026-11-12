package io.quarkiverse.ironjacamar.test.stop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Control case: without idle timeout nor background validation, no pool service is started and the stop is fast.
 */
public class NoPoolServiceStopTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = StopDuration.application()
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.idle-timeout-minutes", "0");

    @Test
    void stopIsFastWithoutThePoolServices() {
        // the stop duration is asserted once the application is stopped
    }
}
