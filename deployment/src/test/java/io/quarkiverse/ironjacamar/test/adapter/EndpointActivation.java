package io.quarkiverse.ironjacamar.test.adapter;

import java.util.Map;

/**
 * An endpoint activation recorded by the {@link RecordingResourceAdapterFactory}.
 *
 * @param containerId the identifier of the container the endpoint was activated in
 * @param endpointClass the activated endpoint class
 * @param config the activation spec configuration
 */
public record EndpointActivation(String containerId, Class<?> endpointClass, Map<String, String> config) {
}
