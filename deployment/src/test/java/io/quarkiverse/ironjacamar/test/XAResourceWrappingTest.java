package io.quarkiverse.ironjacamar.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.xa.XAResource;

import jakarta.inject.Inject;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

import org.jboss.jca.core.spi.transaction.xa.XAResourceWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.ResourceAdapterTypes;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkiverse.ironjacamar.runtime.endpoint.MessageEndpointWrapper;
import io.quarkiverse.ironjacamar.runtime.endpoint.TransactionAwareMessageEndpoint;
import io.quarkiverse.ironjacamar.test.adapter.TestActivationSpec;
import io.quarkiverse.ironjacamar.test.adapter.TestConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestManagedConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceEndpoint;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies that {@link io.quarkiverse.ironjacamar.runtime.endpoint.DefaultMessageEndpointFactory#createEndpoint(XAResource)}
 * wraps the inbound XAResource in an {@link XAResourceWrapper}.
 * <p>
 * This prevents Narayana from attempting to serialize the resource adapter's XAResource
 * during the 2PC prepare phase. Resource adapters like IBM MQ 10 provide XAResource
 * implementations that declare {@link Serializable} but contain non-serializable fields,
 * causing the transaction to abort.
 */
public class XAResourceWrappingTest {

    static final AtomicReference<MessageEndpointFactory> capturedEndpointFactory = new AtomicReference<>();
    static final AtomicReference<XAResource> capturedXAResource = new AtomicReference<>();

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
    IronJacamarContainer container;

    @Test
    void createEndpointShouldWrapXAResource() throws Exception {
        XAResource serializableXAResource = mock(XAResource.class, withSettings().extraInterfaces(Serializable.class));
        capturedEndpointFactory.get().createEndpoint(serializableXAResource);
        assertThat(capturedXAResource.get())
                .isInstanceOf(XAResourceWrapper.class)
                .isNotInstanceOf(Serializable.class);
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

        @Override
        public MessageEndpoint wrap(MessageEndpoint endpoint, Object resourceEndpoint) {
            if (endpoint instanceof MessageEndpointWrapper wrapper
                    && wrapper.unwrap() instanceof TransactionAwareMessageEndpoint txEndpoint) {
                capturedXAResource.set(txEndpoint.getXAResource());
            }
            return endpoint;
        }
    }
}
