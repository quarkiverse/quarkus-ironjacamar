package io.quarkiverse.ironjacamar.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item is created once the IronJacamar container synthetic bean is created, one for each configured
 * resource adapter.
 * <p>
 * Extensions consume it to discover the deployed resource adapters, typically to pick the container id of a
 * {@link ResourceEndpointBuildItem} they contribute.
 */
public final class ContainerCreatedBuildItem extends MultiBuildItem {

    private final String identifier;
    private final String kind;

    /**
     * Constructor
     *
     * @param identifier the identifier of the resource adapter, as configured in
     *        {@code quarkus.ironjacamar.<identifier>.ra}
     * @param kind the kind of the resource adapter
     */
    public ContainerCreatedBuildItem(String identifier, String kind) {
        this.identifier = identifier;
        this.kind = kind;
    }

    /**
     * Get the identifier of the resource adapter, which is also the key of the synthetic bean
     *
     * @return the identifier
     */
    public String identifier() {
        return identifier;
    }

    /**
     * Get the kind of the resource adapter, as declared in the
     * {@link io.quarkiverse.ironjacamar.ResourceAdapterKind} annotation of its factory
     *
     * @return the kind
     */
    public String kind() {
        return kind;
    }
}
