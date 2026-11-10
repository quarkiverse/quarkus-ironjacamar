package io.quarkiverse.ironjacamar.test.stop;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkiverse.ironjacamar.test.adapter.RecordingResourceAdapterFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestActivationSpec;
import io.quarkiverse.ironjacamar.test.adapter.TestConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestManagedConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceAdapter;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Measures how long the application takes to stop, from the {@link ShutdownEvent} to the end of
 * {@code RunningQuarkusApplication.close()}.
 * <p>
 * The application side ({@link ShutdownObserver}) and the JUnit side (the after undeploy listener) are loaded by
 * different class loaders, hence the system property used to share the instant the stop began.
 * <p>
 * The pool services (IdleRemover, ConnectionValidator) run on the Quarkus {@code ManagedExecutor}. Their
 * {@code stop()} never wakes up their runner, so its thread stays in {@code Condition.await} until the Quarkus thread
 * pool interrupts it, after {@code quarkus.thread-pool.shutdown-interrupt} (10 s by default).
 */
final class StopDuration {

    static final Duration MAX_STOP_DURATION = Duration.ofSeconds(3);

    private static final String STOP_BEGIN_PROPERTY = StopDuration.class.getName() + ".begin";

    private StopDuration() {
    }

    /**
     * Create an application with a single test resource adapter, whose stop must take less than
     * {@link #MAX_STOP_DURATION}.
     */
    static QuarkusExtensionTest application() {
        return new QuarkusExtensionTest()
                .withApplicationRoot(root -> root
                        .addClasses(
                                StopDuration.class,
                                ShutdownObserver.class,
                                RecordingResourceAdapterFactory.class,
                                TestResourceAdapter.class,
                                TestManagedConnectionFactory.class,
                                TestConnectionFactory.class,
                                TestActivationSpec.class))
                .overrideConfigKey("quarkus.ironjacamar.ra.kind", "test")
                .setBeforeAllCustomizer(() -> System.clearProperty(STOP_BEGIN_PROPERTY))
                .setAfterUndeployListener(StopDuration::assertStopWasFast);
    }

    private static void assertStopWasFast() {
        long end = System.nanoTime();
        String begin = System.clearProperty(STOP_BEGIN_PROPERTY);
        assertNotNull(begin, "the ShutdownEvent was not observed");
        Duration stop = Duration.ofNanos(end - Long.parseLong(begin));
        assertTrue(stop.compareTo(MAX_STOP_DURATION) < 0,
                "the stop took " + stop.toMillis() + " ms, expected less than " + MAX_STOP_DURATION.toMillis() + " ms");
    }

    @Singleton
    static class ShutdownObserver {
        void onStop(@Observes ShutdownEvent event) {
            System.setProperty(STOP_BEGIN_PROPERTY, Long.toString(System.nanoTime()));
        }
    }
}
