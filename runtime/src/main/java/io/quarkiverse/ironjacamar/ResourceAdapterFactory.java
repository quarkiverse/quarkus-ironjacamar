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
     *
     * @return the name of the resource adapter
     */
    default String getProductName() {
        return toString();
    }

    /**
     * The product version of the resource adapter.
     *
     * @return the version of the resource adapter
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
     * @param id the resource adapter id
     * @param config the configuration subset to be used in {@link ResourceAdapter}
     * @return the resource adapter
     * @throws ResourceException if something goes wrong
     */
    ResourceAdapter createResourceAdapter(String id, Map<String, String> config) throws ResourceException;

    /**
     * Create a managed connection factory for the given resource adapter.
     *
     * @param id the resource adapter id
     * @param adapter the resource adapter
     * @return a {@link ManagedConnectionFactory} instance bound to the given {@link ResourceAdapter}
     * @throws ResourceException if something goes wrong
     */
    ManagedConnectionFactory createManagedConnectionFactory(String id, ResourceAdapter adapter)
            throws ResourceException;

    /**
     * Create an activation spec for the given type.
     *
     * @param id the resource adapter id
     * @param adapter the resource adapter
     * @param type the type
     * @param config the configuration subset to be used in {@link ActivationSpec}
     * @return the activation spec
     * @throws ResourceException if something goes wrong
     */
    ActivationSpec createActivationSpec(String id, ResourceAdapter adapter, Class<?> type, Map<String, String> config)
            throws ResourceException;

    /**
     * In some cases, the ResourceEndpoint requires a specific interface to be implemented
     * (e.g. <code>jakarta.jms.MessageListener</code> for JMS).
     *
     * @param messageEndpoint the resource endpoint (e.g. <code>jakarta.jms.MessageListener</code>)
     * @param instance the instance of the endpoint
     * @return the wrapped endpoint
     */
    default MessageEndpoint wrap(MessageEndpoint messageEndpoint, Object instance) {
        return messageEndpoint;
    }
}
