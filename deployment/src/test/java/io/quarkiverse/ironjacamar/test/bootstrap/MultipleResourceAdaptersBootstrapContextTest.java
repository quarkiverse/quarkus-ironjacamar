package io.quarkiverse.ironjacamar.test.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;

import org.jboss.jca.core.api.workmanager.WorkManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.ironjacamar.Defaults;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkiverse.ironjacamar.runtime.listener.ResourceAdapterLifecycleListener;
import io.quarkiverse.ironjacamar.test.adapter.RecordingResourceAdapterFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestActivationSpec;
import io.quarkiverse.ironjacamar.test.adapter.TestConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestManagedConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceAdapter;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Identifier;

/**
 * Every resource adapter must get its own bootstrap context and its own work manager.
 * <p>
 * A resource adapter shuts its work manager and its bootstrap context down when it stops, so sharing the default
 * ones makes the first resource adapter to stop shut down the work manager the other ones are still using: their
 * work is then rejected with {@code WorkRejectedException}, as {@code WorkManagerImpl.doFirstChecks} refuses to
 * schedule anything once {@code isShutdown()} is true.
 */
public class MultipleResourceAdaptersBootstrapContextTest {

    private static final String OTHER = "other";

    private static final String SHUTDOWN_VIOLATION_PROPERTY = MultipleResourceAdaptersBootstrapContextTest.class
            .getName() + ".violation";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(
                            RecordingResourceAdapterFactory.class,
                            TestResourceAdapter.class,
                            TestManagedConnectionFactory.class,
                            TestConnectionFactory.class,
                            TestActivationSpec.class,
                            WorkManagerShutdownRecorder.class))
            .overrideConfigKey("quarkus.ironjacamar.ra.kind", "test")
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.idle-timeout-minutes", "0")
            .overrideConfigKey("quarkus.ironjacamar." + OTHER + ".ra.kind", "test")
            .overrideConfigKey("quarkus.ironjacamar." + OTHER + ".ra.cm.pool.config.idle-timeout-minutes", "0")
            .setBeforeAllCustomizer(() -> System.clearProperty(SHUTDOWN_VIOLATION_PROPERTY))
            .setAfterUndeployListener(MultipleResourceAdaptersBootstrapContextTest::assertNoWorkManagerWasShutDownTooEarly);

    @Inject
    @Identifier(Defaults.DEFAULT_RESOURCE_ADAPTER_NAME)
    IronJacamarContainer defaultContainer;

    @Inject
    @Identifier(OTHER)
    IronJacamarContainer otherContainer;

    @Test
    void eachResourceAdapterHasItsOwnBootstrapContext() {
        assertThat(bootstrapContextOf(defaultContainer))
                .isNotSameAs(bootstrapContextOf(otherContainer));
    }

    @Test
    void eachResourceAdapterHasItsOwnWorkManager() {
        assertThat(bootstrapContextOf(defaultContainer).getWorkManager())
                .isNotSameAs(bootstrapContextOf(otherContainer).getWorkManager());
    }

    private static BootstrapContext bootstrapContextOf(IronJacamarContainer container) {
        TestResourceAdapter resourceAdapter = (TestResourceAdapter) container.getResourceAdapter();
        // the resource adapters are started asynchronously, each one in its own verticle
        await().atMost(Duration.ofSeconds(10)).until(() -> resourceAdapter.getBootstrapContext() != null);
        return resourceAdapter.getBootstrapContext();
    }

    /**
     * The application side ({@link WorkManagerShutdownRecorder}) and the JUnit side are loaded by different class
     * loaders, hence the system property used to report a violation observed during the shutdown.
     */
    private static void assertNoWorkManagerWasShutDownTooEarly() {
        String violation = System.clearProperty(SHUTDOWN_VIOLATION_PROPERTY);
        if (violation != null) {
            throw new AssertionError(violation);
        }
    }

    /**
     * Records the resource adapters whose work manager was already shut down when they were stopped, which is what
     * happens to every resource adapter but the first one to stop when they share the default bootstrap context.
     */
    @Singleton
    static class WorkManagerShutdownRecorder implements ResourceAdapterLifecycleListener {

        @Override
        public void preShutdown(String id, ResourceAdapter resourceAdapter) {
            BootstrapContext bootstrapContext = ((TestResourceAdapter) resourceAdapter).getBootstrapContext();
            if (bootstrapContext != null
                    && bootstrapContext.getWorkManager() instanceof WorkManager workManager
                    && workManager.isShutdown()) {
                System.setProperty(SHUTDOWN_VIOLATION_PROPERTY,
                        "the work manager of the resource adapter '" + id + "' was already shut down when it stopped");
            }
        }
    }
}
