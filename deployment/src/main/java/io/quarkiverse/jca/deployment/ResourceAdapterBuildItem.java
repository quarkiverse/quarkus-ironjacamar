package io.quarkiverse.jca.deployment;

import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents a JCA resource adapter that should be deployed.
 */
public final class ResourceAdapterBuildItem extends MultiBuildItem {

    public final String className;

    public final Set<String> endpointsClassNames;

    public ResourceAdapterBuildItem(String className, Set<String> endpointsClassNames) {
        this.className = className;
        this.endpointsClassNames = endpointsClassNames;
    }
}
