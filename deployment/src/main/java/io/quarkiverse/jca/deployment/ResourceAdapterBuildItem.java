package io.quarkiverse.jca.deployment;

import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents a JCA resource adapter that should be deployed.
 */
public final class ResourceAdapterBuildItem extends MultiBuildItem {

    public final String resourceAdapterClassName;

    public final Set<String> endpointClassnames;

    public ResourceAdapterBuildItem(String className, Set<String> endpointClasses) {
        //TODO: Create a builder for this class
        this.resourceAdapterClassName = className;
        this.endpointClassnames = endpointClasses;
    }
}
