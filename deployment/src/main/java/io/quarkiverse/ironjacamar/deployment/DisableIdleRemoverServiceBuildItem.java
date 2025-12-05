package io.quarkiverse.ironjacamar.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that represents the intention to disable the idle remover service.
 * <p>
 * This build item can be used by extensions or internal components to signal
 * that the idle remover service should not be used. It ensures control over
 * the behavior related to the idle remover to potentially improve performance
 * or adapt to specific runtime requirements.
 */
public final class DisableIdleRemoverServiceBuildItem extends SimpleBuildItem {
}
