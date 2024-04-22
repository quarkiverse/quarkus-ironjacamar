package io.quarkiverse.ironjacamar.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.logging.Level;

import jakarta.inject.Inject;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkiverse.ironjacamar.test.adapter.TestActivationSpec;
import io.quarkiverse.ironjacamar.test.adapter.TestManagedConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceAdapter;
import io.quarkus.test.QuarkusUnitTest;

public class MissingResourceAdapterTypesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(MissingResourceAdapterTypesResourceAdapterFactory.class,
                            TestResourceAdapter.class,
                            TestManagedConnectionFactory.class,
                            TestActivationSpec.class))
            .overrideConfigKey("quarkus.ironjacamar.ra.kind", "test")
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.FINER.intValue()
                    && record.getMessage().contains("QIJ000012"))
            .assertLogRecords(records -> {
                assertThat(records).isNotEmpty();
            });

    @Inject
    IronJacamarContainer ironJacamarContainer;

    @Test
    public void shouldStartResource() {
        TestResourceAdapter testResourceAdapter = (TestResourceAdapter) ironJacamarContainer.getResourceAdapter();
        assertTrue(testResourceAdapter.isStarted());
    }

    @ResourceAdapterKind("test")
    static class MissingResourceAdapterTypesResourceAdapterFactory implements ResourceAdapterFactory {

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
