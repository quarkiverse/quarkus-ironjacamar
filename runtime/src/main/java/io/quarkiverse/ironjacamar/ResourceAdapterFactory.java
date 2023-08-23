package io.quarkiverse.ironjacamar;

import java.util.Map;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.endpoint.MessageEndpoint;

/**
 * SPI for configuring the resource adapter.
 * <p>
 * Every Resource adapter must provide a {@link jakarta.inject.Singleton} CDI bean implementing this interface.
 */
public interface ResourceAdapterFactory<RA extends ResourceAdapter> {

    /**
     * Create and configure the resource adapter.
     *
     * @param config the configuration subset to be used in {@link ResourceAdapter}
     */
    RA createResourceAdapter(Map<String, String> config);

    /**
     * Create a managed connection factory for the given resource adapter.
     *
     * @param adapter the resource adapter
     * @return a {@link ManagedConnectionFactory} instance bound to the given {@link ResourceAdapter}
     * @throws ResourceException if something goes wrong
     */
    ManagedConnectionFactory createManagedConnectionFactory(Map<String, String> config, RA adapter) throws ResourceException;

    /**
     * Create an activation spec for the given type.
     *
     * @param type the type
     * @return the activation spec
     */
    ActivationSpec createActivationSpec(Map<String, String> config, RA adapter, Class<?> type) throws Exception;

    /**
     * In some cases, the ResourceEndpoint requires a specific interface to be implemented
     * (e.g. {@link jakarta.jms.MessageListener} for JMS).
     *
     * @param resourceEndpoint the resource endpoint (e.g. {@link jakarta.jms.MessageListener})
     * @param endpoint the {@link MessageEndpoint} to wrap
     */
    default MessageEndpoint wrap(Object resourceEndpoint, MessageEndpoint endpoint) {
        return endpoint;
    }

}
