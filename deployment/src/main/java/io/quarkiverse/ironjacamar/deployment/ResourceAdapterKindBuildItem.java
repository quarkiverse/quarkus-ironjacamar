package io.quarkiverse.ironjacamar.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents a JCA resource adapter kind.
 */
public final class ResourceAdapterKindBuildItem extends MultiBuildItem {

    public final String kind;
    public final String resourceAdapterFactoryClassName;

    public ResourceAdapterKindBuildItem(String kind, String resourceAdapterFactoryClassName) {
        this.kind = kind;
        this.resourceAdapterFactoryClassName = resourceAdapterFactoryClassName;
    }

    public String getKind() {
        return kind;
    }
}
