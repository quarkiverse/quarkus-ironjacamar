package io.quarkiverse.ironjacamar.deployment;

import io.quarkiverse.ironjacamar.Defaults;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * A {@link MessageEndpointActivationBuildItem} activates {@link jakarta.resource.spi.endpoint.MessageEndpoint}
 * in a {@link jakarta.resource.spi.ResourceAdapter}
 */
public final class MessageEndpointActivationBuildItem extends MultiBuildItem {
    private final String resourceAdapterId;
    private final String activationSpecConfigId;
    private final String endpointClassName;

    public MessageEndpointActivationBuildItem(String activationSpecConfigId, String endpointClassName) {
        this.resourceAdapterId = Defaults.DEFAULT_RESOURCE_ADAPTER_NAME;
        this.activationSpecConfigId = activationSpecConfigId;
        this.endpointClassName = endpointClassName;
    }

    public MessageEndpointActivationBuildItem(String resourceAdapterId, String activationSpecConfigId,
            String endpointClassName) {
        this.resourceAdapterId = resourceAdapterId;
        this.activationSpecConfigId = activationSpecConfigId;
        this.endpointClassName = endpointClassName;
    }

    public String getResourceAdapterId() {
        return resourceAdapterId;
    }

    public String getActivationSpecConfigId() {
        return activationSpecConfigId;
    }

    public String getEndpointClassName() {
        return endpointClassName;
    }
}
