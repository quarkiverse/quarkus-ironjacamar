package io.quarkiverse.ironjacamar.deployment;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents a JCA resource adapter that should be deployed.
 */
public final class ResourceAdapterBuildItem extends MultiBuildItem {

    public final String resourceAdapterClassName;

    public final Set<String> endpointClassnames;

    ResourceAdapterBuildItem(String className, Set<String> endpointClasses) {
        this.resourceAdapterClassName = className;
        this.endpointClassnames = endpointClasses;
    }

    public static Builder builder(String className) {
        return new Builder(className);
    }

    public static class Builder {
        private final String resourceAdapterClassName;

        private final Set<String> endpointClassnames = new HashSet<>();

        public Builder(String className) {
            this.resourceAdapterClassName = Objects.requireNonNull(className, "className must not be null");
        }

        public Builder addEndpoints(Set<String> endpointClassNames) {
            this.endpointClassnames.addAll(Objects.requireNonNull(endpointClassNames, "endpointClassNames must not be null"));
            return this;
        }

        public ResourceAdapterBuildItem build() {
            return new ResourceAdapterBuildItem(resourceAdapterClassName, endpointClassnames);
        }

    }
}
