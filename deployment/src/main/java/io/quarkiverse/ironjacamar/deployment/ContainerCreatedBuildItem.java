package io.quarkiverse.ironjacamar.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item is created once the IronJacamar container synthetic bean is created.
 */
final class ContainerCreatedBuildItem extends MultiBuildItem {

    /**
     * The key of the synthetic bean.
     */
    public final String key;

    /**
     * The kind of the synthetic bean.
     */
    public final String kind;

    public ContainerCreatedBuildItem(String key, String kind) {
        this.key = key;
        this.kind = kind;
    }
}
