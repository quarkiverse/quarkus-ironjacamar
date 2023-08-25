package io.quarkiverse.ironjacamar.deployment;

import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
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

    @SuppressWarnings({ "unchecked" })
    public Class<? extends ResourceAdapterFactory> getResourceAdapterFactoryClass() {
        try {
            return (Class<? extends ResourceAdapterFactory>) Class.forName(resourceAdapterFactoryClassName, true,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public ResourceAdapterKind getResourceAdapterKind() {
        return new ResourceAdapterKind.Literal(kind);
    }
}
