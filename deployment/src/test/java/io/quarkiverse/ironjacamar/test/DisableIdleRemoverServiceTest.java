package io.quarkiverse.ironjacamar.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

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
import io.quarkiverse.ironjacamar.runtime.IdleRemoverManager;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkiverse.ironjacamar.test.adapter.TestActivationSpec;
import io.quarkiverse.ironjacamar.test.adapter.TestConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestManagedConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceAdapter;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceEndpoint;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class DisableIdleRemoverServiceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(TestResourceAdapterFactory.class,
                            TestResourceAdapter.class,
                            TestManagedConnectionFactory.class,
                            TestActivationSpec.class,
                            TestConnectionFactory.class,
                            TestResourceEndpoint.class))
            .addBuildChainCustomizer(builder -> {
                try {
                    Class<?> disableIdleRemoverBuildItem = Class
                            .forName("io.quarkiverse.ironjacamar.deployment.DisableIdleRemoverServiceBuildItem");
                    builder.addBuildStep(context -> {
                        try {
                            Object item = disableIdleRemoverBuildItem.getDeclaredConstructor().newInstance();
                            Class<?> buildItem = context.getClass().getClassLoader()
                                    .loadClass("io.quarkus.builder.item.BuildItem");
                            context.getClass().getMethod("produce", buildItem).invoke(context, item);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).produces((Class) disableIdleRemoverBuildItem).build();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            })
            .overrideConfigKey("quarkus.ironjacamar.ra.kind", "test");

    @Inject
    IronJacamarContainer ironJacamarContainer;

    @Test
    void shouldDisableIdleRemoverServiceAndStartResourceAdapter() {
        assertFalse(Arc.container().instance(IdleRemoverManager.class).isAvailable());

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
