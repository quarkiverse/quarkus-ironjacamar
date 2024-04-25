package io.quarkiverse.ironjacamar.test;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.ResourceAdapterTypes;
import io.quarkiverse.ironjacamar.runtime.listener.ResourceAdapterLifecycleListener;
import io.quarkiverse.ironjacamar.test.adapter.TestActivationSpec;
import io.quarkiverse.ironjacamar.test.adapter.TestConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestManagedConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceAdapter;
import io.quarkus.test.QuarkusUnitTest;

public class LifecycleTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(
                            TestResourceAdapterLifecycleListener.class,
                            TestResourceAdapterFactory.class,
                            TestResourceAdapter.class,
                            TestManagedConnectionFactory.class,
                            TestConnectionFactory.class,
                            TestActivationSpec.class))
            .overrideConfigKey("quarkus.ironjacamar.ra.kind", "test");

    @Test
    void shouldInvokePrecondition() {
        await().atMost(Duration.ofSeconds(10)).untilTrue(TestResourceAdapterLifecycleListener.started);
    }

    @ResourceAdapterKind("test")
    @ResourceAdapterTypes(connectionFactoryTypes = TestConnectionFactory.class)
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
        public ManagedConnectionFactory createManagedConnectionFactory(String id, ResourceAdapter adapter) {
            return new TestManagedConnectionFactory();
        }

        @Override
        public ActivationSpec createActivationSpec(String id, ResourceAdapter adapter, Class<?> type,
                Map<String, String> config) {
            return new TestActivationSpec(adapter);
        }
    }

    @ApplicationScoped
    static class TestResourceAdapterLifecycleListener implements ResourceAdapterLifecycleListener {

        public static AtomicBoolean started = new AtomicBoolean();

        @Override
        public void preStartup(String id, ResourceAdapter resourceAdapter) {
            started.set(true);
        }
    }
}
