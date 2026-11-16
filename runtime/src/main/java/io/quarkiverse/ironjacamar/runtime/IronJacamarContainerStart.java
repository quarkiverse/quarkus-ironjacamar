package io.quarkiverse.ironjacamar.runtime;

/**
 * Holds the id of the deployment of the verticle running a started resource adapter.
 * <p>
 * The service holding it completes once the resource adapter is started, so requiring it guarantees that the
 * resource adapter is running.
 *
 * @param deploymentId the id of the deployment of the resource adapter verticle
 */
public record IronJacamarContainerStart(String deploymentId) {
}
