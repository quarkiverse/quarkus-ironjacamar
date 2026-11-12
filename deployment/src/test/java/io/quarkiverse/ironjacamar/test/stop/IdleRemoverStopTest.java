package io.quarkiverse.ironjacamar.test.stop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * The IdleRemover is started with the default configuration ({@code idle-timeout-minutes=30}): the stop of the
 * application must not wait for the Quarkus thread pool to interrupt its thread.
 */
public class IdleRemoverStopTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = StopDuration.application();

    @Test
    void stopIsFastWithTheDefaultConfiguration() {
        // the stop duration is asserted once the application is stopped
    }
}
