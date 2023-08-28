package io.quarkiverse.ironjacamar.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.core.Future;

/**
 * This build item is created once the IronJacamar container is started.
 */
final class ContainerStartedBuildItem extends MultiBuildItem {
    /**
     * The key of the synthetic bean.
     */
    public final String identifier;

    public final RuntimeValue<Future<String>> futureRuntimeValue;

    public ContainerStartedBuildItem(String identifier, RuntimeValue<Future<String>> futureRuntimeValue) {
        this.identifier = identifier;
        this.futureRuntimeValue = futureRuntimeValue;
    }
}
