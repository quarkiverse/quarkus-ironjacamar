package io.quarkiverse.jca.deployment;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents a JCA resource adapter that should be deployed.
 */
public final class ResourceAdapterBuildItem extends MultiBuildItem {

    public final ClassInfo classInfo;

    public ResourceAdapterBuildItem(ClassInfo classInfo) {
        this.classInfo = classInfo;
    }
}
