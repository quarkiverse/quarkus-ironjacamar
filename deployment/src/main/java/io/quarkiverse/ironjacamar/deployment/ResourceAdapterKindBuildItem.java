package io.quarkiverse.ironjacamar.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents a JCA resource adapter kind.
 */
public final class ResourceAdapterKindBuildItem extends MultiBuildItem {

    private final String kind;
    private final String resourceAdapterFactoryClassName;

    /**
     * Constructor
     *
     * @param kind the kind
     * @param resourceAdapterFactoryClassName the resource adapter factory class name
     */
    public ResourceAdapterKindBuildItem(String kind, String resourceAdapterFactoryClassName) {
        this.kind = kind;
        this.resourceAdapterFactoryClassName = resourceAdapterFactoryClassName;
    }

    /**
     * Get the kind
     *
     * @return the kind
     */
    public String kind() {
        return kind;
    }

    /**
     * Get the resource adapter factory class name
     *
     * @return the resource adapter factory class name
     */
    public String resourceAdapterFactoryClassName() {
        return resourceAdapterFactoryClassName;
    }
}
