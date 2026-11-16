package io.quarkiverse.ironjacamar.test.adapter;

import java.util.Map;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.ResourceAdapterTypes;

/**
 * A {@link ResourceAdapterFactory} providing a {@link FailingResourceAdapter} that starts, but fails to activate
 * its endpoints.
 * <p>
 * Only one factory of the {@code test} kind can be deployed in an application, so this one does not extend
 * {@link FailingResourceAdapterFactory}: both would be discovered.
 */
@ResourceAdapterKind("test")
@ResourceAdapterTypes(connectionFactoryTypes = TestConnectionFactory.class)
public class FailingEndpointActivationResourceAdapterFactory implements ResourceAdapterFactory {

    @Override
    public String getProductName() {
        return "Test Resource Adapter failing the endpoint activation";
    }

    @Override
    public ResourceAdapter createResourceAdapter(String id, Map<String, String> config) {
        return new FailingResourceAdapter(false);
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
