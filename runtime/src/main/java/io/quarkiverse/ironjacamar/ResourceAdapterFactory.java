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
 * Every Resource adapter must provide an implementation of this interface.
 */
public interface ResourceAdapterFactory {

    /**
     * The product name of the resource adapter.
     */
    default String getProductName() {
        return toString();
    }

    /**
     * The product version of the resource adapter.
     */
    default String getProductVersion() {
        return "1.0";
    }

    /**
     * A human-readable description of the resource adapter.
     *
     * @return the description that is displayed in the logs
     */
    default String getDescription() {
        return getProductName() + " " + getProductVersion();
    }

    /**
     * Create and configure the resource adapter.
     *
     * @param config the configuration subset to be used in {@link ResourceAdapter}
     */
    ResourceAdapter createResourceAdapter(String id, Map<String, String> config) throws ResourceException;

    /**
     * Create a managed connection factory for the given resource adapter.
     *
     * @param adapter the resource adapter
     * @return a {@link ManagedConnectionFactory} instance bound to the given {@link ResourceAdapter}
     * @throws ResourceException if something goes wrong
     */
    ManagedConnectionFactory createManagedConnectionFactory(String id, ResourceAdapter adapter)
            throws ResourceException;

    /**
     * Create an activation spec for the given type.
     *
     * @param type the type
     * @return the activation spec
     */
    ActivationSpec createActivationSpec(String id, ResourceAdapter adapter, Class<?> type, Map<String, String> config)
            throws ResourceException;

    /**
     * In some cases, the ResourceEndpoint requires a specific interface to be implemented
     * (e.g. {@link jakarta.jms.MessageListener} for JMS).
     *
     * @param messageEndpoint the resource endpoint (e.g. {@link jakarta.jms.MessageListener})
     * @param instance the instance of the endpoint
     */
    default MessageEndpoint wrap(MessageEndpoint messageEndpoint, Object instance) {
        return messageEndpoint;
    }
}
