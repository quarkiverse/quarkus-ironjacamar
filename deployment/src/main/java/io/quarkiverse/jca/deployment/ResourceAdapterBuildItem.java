package io.quarkiverse.jca.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents a JCA resource adapter that should be deployed.
 */
public final class ResourceAdapterBuildItem extends MultiBuildItem {

    public final String className;

    public ResourceAdapterBuildItem(String className) {
        this.className = className;
    }
}
