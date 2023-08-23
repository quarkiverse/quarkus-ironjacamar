package io.quarkiverse.ironjacamar.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents a JCA resource adapter kind.
 */
public final class ResourceAdapterKindBuildItem extends MultiBuildItem {

    public final String kind;

    public ResourceAdapterKindBuildItem(String kind) {
        this.kind = kind;
    }
}
