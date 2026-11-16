package io.quarkiverse.ironjacamar.test.adapter;

import java.util.Map;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.ResourceAdapterTypes;

/**
 * A {@link ResourceAdapterFactory} providing a {@link FailingResourceAdapter} that fails to start.
 * <p>
 * Extend it and override {@link #createResourceAdapter(String, Map)} for a resource adapter that starts, but fails
 * to activate its endpoints.
 */
@ResourceAdapterKind("test")
@ResourceAdapterTypes(connectionFactoryTypes = TestConnectionFactory.class)
public class FailingResourceAdapterFactory implements ResourceAdapterFactory {

    @Override
    public String getProductName() {
        return "Failing Test Resource Adapter";
    }

    @Override
    public ResourceAdapter createResourceAdapter(String id, Map<String, String> config) {
        return new FailingResourceAdapter(true);
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
