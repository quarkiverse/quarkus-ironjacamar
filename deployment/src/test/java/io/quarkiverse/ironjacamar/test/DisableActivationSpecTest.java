package io.quarkiverse.ironjacamar.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

public class DisableActivationSpecTest {

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
            .overrideConfigKey("quarkus.ironjacamar.activation-spec.test.enabled", "false");

    @Inject
    IronJacamarContainer ironJacamarContainer;

    @Test
    public void shouldStartResource() {
        TestResourceAdapterFactory resourceAdapterFactory = (TestResourceAdapterFactory) ironJacamarContainer
                .resourceAdapterFactory();
        TestResourceAdapter testResourceAdapter = (TestResourceAdapter) ironJacamarContainer.resourceAdapter();
        assertTrue(testResourceAdapter.isStarted());
        assertThat(resourceAdapterFactory.getActivationCount()).isEqualTo(0);
    }

    @ResourceAdapterKind("test")
    @ResourceAdapterTypes(connectionFactoryTypes = { TestConnectionFactory.class })
    static class TestResourceAdapterFactory implements ResourceAdapterFactory {

        AtomicInteger activationCount = new AtomicInteger(0);

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
            activationCount.incrementAndGet();
            return new TestActivationSpec(adapter);
        }

        public int getActivationCount() {
            return activationCount.get();
        }
    }
}
