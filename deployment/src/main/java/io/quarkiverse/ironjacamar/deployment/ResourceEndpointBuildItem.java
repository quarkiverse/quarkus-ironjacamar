package io.quarkiverse.ironjacamar.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a generated {@code @ResourceEndpoint} class to be activated
 * by IronJacamar. Used for endpoints generated at build time (e.g., from
 * {@code @JmsListener} methods) that are not in the Jandex index at the
 * time {@code registerResourceEndpoints} runs.
 */
public final class ResourceEndpointBuildItem extends MultiBuildItem {

    private final String endpointClassName;
    private final String activationSpecKey;
    private final String containerId;

    public ResourceEndpointBuildItem(String endpointClassName, String activationSpecKey) {
        this(endpointClassName, activationSpecKey, null);
    }

    public ResourceEndpointBuildItem(String endpointClassName, String activationSpecKey, String containerId) {
        this.endpointClassName = endpointClassName;
        this.activationSpecKey = activationSpecKey;
        this.containerId = containerId;
    }

    public String getEndpointClassName() {
        return endpointClassName;
    }

    public String getActivationSpecKey() {
        return activationSpecKey;
    }

    public String getContainerId() {
        return containerId;
    }
}
