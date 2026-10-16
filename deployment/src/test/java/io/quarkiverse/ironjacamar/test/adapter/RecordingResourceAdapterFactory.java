package io.quarkiverse.ironjacamar.test.adapter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.ResourceAdapterTypes;

/**
 * A {@link ResourceAdapterFactory} recording every endpoint activation, so that tests can assert which endpoints were
 * activated and in which container.
 */
@ResourceAdapterKind("test")
@ResourceAdapterTypes(connectionFactoryTypes = { TestConnectionFactory.class })
public class RecordingResourceAdapterFactory implements ResourceAdapterFactory {

    private final List<EndpointActivation> activations = new CopyOnWriteArrayList<>();

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
        activations.add(new EndpointActivation(id, type, Map.copyOf(config)));
        return new TestActivationSpec(adapter);
    }

    /**
     * Get the recorded endpoint activations
     *
     * @return the recorded endpoint activations
     */
    public List<EndpointActivation> getActivations() {
        return activations;
    }
}
