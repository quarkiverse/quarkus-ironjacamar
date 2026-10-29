package io.quarkiverse.ironjacamar.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item is created once the IronJacamar container is started.
 */
final class ContainerStartedBuildItem extends MultiBuildItem {

    private final String identifier;
    private final String startServiceName;

    ContainerStartedBuildItem(String identifier, String startServiceName) {
        this.identifier = identifier;
        this.startServiceName = startServiceName;
    }

    /**
     * Get the identifier of the resource adapter, which is also the key of the synthetic bean
     *
     * @return the identifier
     */
    String identifier() {
        return identifier;
    }

    /**
     * Get the name of the service that deploys the resource adapter and completes with its deployment future
     *
     * @return the start service name
     */
    String startServiceName() {
        return startServiceName;
    }
}
