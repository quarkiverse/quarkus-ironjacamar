package io.quarkiverse.ironjacamar.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.MessageListener;
import jakarta.resource.cci.Record;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.ResourceAdapterTypes;
import io.quarkiverse.ironjacamar.test.adapter.TestActivationSpec;
import io.quarkiverse.ironjacamar.test.adapter.TestConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestManagedConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceEndpoint;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

/**
 * Verifies that {@link io.quarkiverse.ironjacamar.runtime.endpoint.DuplicatedContextMessageEndpoint}
 * correctly restores the previous Vert.x thread-local context after message delivery.
 * <p>
 * The {@code beforeDelivery}/{@code afterDelivery} lifecycle must:
 * <ul>
 * <li>Store the duplicated context so that {@code endDispatch} is called on the same instance</li>
 * <li>Capture and restore the previous thread-local context rather than always clearing it to {@code null}</li>
 * </ul>
 */
public class DuplicatedContextRestorationTest {

    static final AtomicReference<MessageEndpointFactory> capturedEndpointFactory = new AtomicReference<>();

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(
                            TestResourceAdapterFactory.class,
                            TestManagedConnectionFactory.class,
                            TestConnectionFactory.class,
                            TestActivationSpec.class,
                            TestResourceEndpoint.class))
            .overrideConfigKey("quarkus.ironjacamar.ra.kind", "test")
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.idle-timeout-minutes", "0");

    @Inject
    Vertx vertx;

    @Test
    void afterDeliveryShouldRestorePreviousContext() throws Exception {
        MessageEndpoint endpoint = capturedEndpointFactory.get().createEndpoint(null);

        // Simulate a pre-existing Vert.x context on the current thread
        Context outerContext = vertx.getOrCreateContext();
        ContextInternal outerContextInternal = (ContextInternal) outerContext;
        outerContextInternal.beginDispatch();
        assertThat(Vertx.currentContext())
                .as("outer context should be set before delivery")
                .isSameAs(outerContext);

        endpoint.beforeDelivery(MessageListener.class.getMethod("onMessage", Record.class));

        assertThat(Vertx.currentContext())
                .as("during delivery, context should be a different (duplicated) instance")
                .isNotNull()
                .isNotSameAs(outerContext);

        endpoint.afterDelivery();

        assertThat(Vertx.currentContext())
                .as("outer context should be restored after delivery")
                .isSameAs(outerContext);
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
            ResourceAdapter adapter = mock(ResourceAdapter.class);
            doAnswer(invocation -> {
                capturedEndpointFactory.set(invocation.getArgument(0));
                return null;
            }).when(adapter).endpointActivation(any(), any());
            return adapter;
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
}
