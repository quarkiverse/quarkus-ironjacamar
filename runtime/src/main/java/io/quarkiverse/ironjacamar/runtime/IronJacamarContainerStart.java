package io.quarkiverse.ironjacamar.runtime;

import io.vertx.core.Future;

/**
 * Holds the deployment {@link Future} of a started IronJacamar container.
 * <p>
 * Used as a service value, since generic types such as {@link Future} cannot be registered as services.
 *
 * @param future the future that completes when the resource adapter verticle is deployed
 */
public record IronJacamarContainerStart(Future<String> future) {
}
