package io.quarkiverse.ironjacamar.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.ResourceAdapterTypes;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkiverse.ironjacamar.test.adapter.TestActivationSpec;
import io.quarkiverse.ironjacamar.test.adapter.TestConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestManagedConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceAdapter;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceEndpoint;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies that {@link ResourceAdapter#endpointDeactivation} is called for every activated endpoint
 * when the application shuts down, as required by the JCA specification.
 *
 * <p>
 * This test uses a custom {@link Handler} instead of {@link QuarkusUnitTest#assertLogRecords} because
 * the deactivation log (QIJ000018) is emitted during CDI bean destruction, which happens inside
 * {@code RunningQuarkusApplication.close()}. The {@code assertLogRecords} mechanism snapshots log records
 * <em>before</em> the application is closed, so it cannot capture shutdown-time messages.
 */
public class EndpointDeactivationTest {

    private static final String LOGGER_CATEGORY = "io.quarkiverse.ironjacamar.runtime";

    private static final List<LogRecord> shutdownRecords = new CopyOnWriteArrayList<>();

    /**
     * Captures the endpoint deactivation log message (QIJ000018) emitted during application shutdown.
     */
    private static final Handler deactivationHandler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            if (record.getMessage() != null && record.getMessage().contains("QIJ000018")) {
                shutdownRecords.add(record);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(TestResourceAdapterFactory.class,
                            TestResourceAdapter.class,
                            TestManagedConnectionFactory.class,
                            TestActivationSpec.class,
                            TestConnectionFactory.class,
                            TestResourceEndpoint.class))
            .overrideConfigKey("quarkus.ironjacamar.ra.kind", "test")
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.idle-timeout-minutes", "0")
            .setBeforeAllCustomizer(() -> {
                shutdownRecords.clear();
                Logger.getLogger(LOGGER_CATEGORY).addHandler(deactivationHandler);
            })
            .setAfterUndeployListener(() -> {
                Logger.getLogger(LOGGER_CATEGORY).removeHandler(deactivationHandler);
                assertThat(shutdownRecords)
                        .as("ResourceAdapter.endpointDeactivation() should have been called during shutdown")
                        .isNotEmpty();
            });

    @Inject
    IronJacamarContainer ironJacamarContainer;

    @Test
    public void shouldDeactivateEndpointOnShutdown() {
        TestResourceAdapter testResourceAdapter = (TestResourceAdapter) ironJacamarContainer.getResourceAdapter();
        assertTrue(testResourceAdapter.isStarted());
    }

    @ResourceAdapterKind("test")
    @ResourceAdapterTypes(connectionFactoryTypes = { TestConnectionFactory.class })
    static class TestResourceAdapterFactory implements ResourceAdapterFactory {

        @Override
        public String getProductName() {
            return "Test Resource Adapter";
        }

        @Override
        public ResourceAdapter createResourceAdapter(String id, Map<String, String> config) throws ResourceException {
            return new TestResourceAdapter();
        }

        @Override
        public ManagedConnectionFactory createManagedConnectionFactory(String id, ResourceAdapter adapter)
                throws ResourceException {
            return new TestManagedConnectionFactory();
        }

        @Override
        public ActivationSpec createActivationSpec(String id, ResourceAdapter adapter, Class<?> type,
                Map<String, String> config) throws ResourceException {
            return new TestActivationSpec(adapter);
        }
    }
}
