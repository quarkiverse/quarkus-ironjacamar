package io.quarkiverse.ironjacamar.runtime.listener;

import jakarta.resource.spi.ResourceAdapter;

/**
 * A listener for resource adapter lifecycle events.
 */
public interface ResourceAdapterLifecycleListener {

    /**
     * Invoked before the resource adapter is started.
     *
     * @param id the resource adapter id
     * @param resourceAdapter the resource adapter
     */
    default void preStartup(String id, ResourceAdapter resourceAdapter) {
    }

    /**
     * Invoked after the resource adapter is started.
     *
     * @param id the resource adapter id
     * @param resourceAdapter the resource adapter
     */
    default void postStartup(String id, ResourceAdapter resourceAdapter) {
    }

    /**
     * Invoked before the resource adapter is stopped.
     *
     * @param id the resource adapter id
     * @param resourceAdapter the resource adapter
     */
    default void preShutdown(String id, ResourceAdapter resourceAdapter) {
    }

    /**
     * Invoked after the resource adapter is stopped.
     *
     * @param id the resource adapter id
     * @param resourceAdapter the resource adapter
     */
    default void postShutdown(String id, ResourceAdapter resourceAdapter) {
    }
}
