package io.quarkiverse.ironjacamar.deployment;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents a JCA resource adapter that should be deployed.
 */
@Deprecated
public final class ResourceAdapterBuildItem extends MultiBuildItem {

    public final String resourceAdapterKind;

    public final Set<String> endpointClassnames;

    ResourceAdapterBuildItem(String resourceAdapterKind, Set<String> endpointClasses) {
        this.resourceAdapterKind = resourceAdapterKind;
        this.endpointClassnames = endpointClasses;
    }

    public static Builder builder(String className) {
        return new Builder(className);
    }

    public static class Builder {
        private final String resourceAdapterKind;

        private final Set<String> endpointClassnames = new HashSet<>();

        public Builder(String kind) {
            this.resourceAdapterKind = Objects.requireNonNull(kind, "kind must not be null");
        }

        public Builder addEndpoints(Set<String> endpointClassNames) {
            this.endpointClassnames.addAll(Objects.requireNonNull(endpointClassNames, "endpointClassNames must not be null"));
            return this;
        }

        public ResourceAdapterBuildItem build() {
            return new ResourceAdapterBuildItem(resourceAdapterKind, endpointClassnames);
        }

    }
}
