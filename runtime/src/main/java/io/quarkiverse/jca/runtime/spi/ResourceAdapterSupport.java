package io.quarkiverse.jca.runtime.spi;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.endpoint.MessageEndpoint;

/**
 * SPI for configuring the resource adapter.
 */
public interface ResourceAdapterSupport {

    /**
     * Configure the resource adapter.
     * <p>
     * This is called before the resource adapter starts
     *
     * @param resourceAdapter the resource adapter
     */
    void configureResourceAdapter(ResourceAdapter resourceAdapter);

    /**
     * Create an activation spec for the given type.
     *
     * @param type the type
     * @return the activation spec
     */
    ActivationSpec createActivationSpec(ResourceAdapter adapter, Class<?> type) throws Exception;

    /**
     * In some cases, the ResourceEndpoint requires a specific interface to be implemented
     */
    default MessageEndpoint wrap(Object resourceEndpoint, MessageEndpoint endpoint) {
        return endpoint;
    }

}
