package io.quarkiverse.ironjacamar.test.start;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

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
 * With {@code fail-on-start-error} disabled, an endpoint that cannot be activated must not fail the startup of the
 * application: the failure is logged and the application starts.
 */
public class LenientEndpointActivationTest {

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
            .overrideConfigKey("quarkus.ironjacamar.ra.fail-on-start-error", "false")
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LogRecord::getMessage)
                    .anyMatch(message -> message.contains("QIJ000025")));

    @Test
    void theApplicationStartsWithoutTheEndpoint() {
        // the application starting is the assertion, the log records are asserted once it is stopped
    }
}
