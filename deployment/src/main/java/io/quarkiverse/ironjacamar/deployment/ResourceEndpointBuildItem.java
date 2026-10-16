package io.quarkiverse.ironjacamar.deployment;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that registers an additional message endpoint to be activated by IronJacamar.
 * <p>
 * Extensions produce this build item for endpoints that are not discovered by the
 * {@link io.quarkiverse.ironjacamar.ResourceEndpoint} annotation scan, typically because they are generated at build
 * time and therefore are not present in the Jandex index.
 * <p>
 * The extension producing this build item is responsible for making the endpoint class an (unremovable) bean.
 * Reflective access to the endpoint methods is registered by this extension.
 * <p>
 * The deployed resource adapters, and therefore the container ids that may be used here, are discovered by consuming
 * {@link ContainerCreatedBuildItem}:
 *
 * <pre>{@code
 * // @BuildStep
 * void contributeEndpoints(List<ContainerCreatedBuildItem> containers,
 *         BuildProducer<GeneratedBeanBuildItem> generatedBeans,
 *         BuildProducer<ResourceEndpointBuildItem> resourceEndpoints) {
 *     for (ContainerCreatedBuildItem container : containers) {
 *         if (!"artemis".equals(container.kind())) {
 *             continue;
 *         }
 *         // Generate the endpoint class as an unremovable bean, then have it activated
 *         String endpointClassName = generateEndpoint(generatedBeans, container);
 *         resourceEndpoints.produce(new ResourceEndpointBuildItem(endpointClassName,
 *                 "my-activation-spec", container.identifier()));
 *     }
 * }
 * }</pre>
 *
 * The {@code my-activation-spec} key above is what the application configures through
 * {@code quarkus.ironjacamar.activation-spec.my-activation-spec.config.*}. Defaults for those properties can be
 * registered with a {@code RunTimeConfigurationDefaultBuildItem}.
 */
public final class ResourceEndpointBuildItem extends MultiBuildItem {

    private final String endpointClassName;
    private final String activationSpecConfigKey;
    private final String containerId;

    /**
     * Constructor for an endpoint activated in the single resource adapter deployed in the application.
     *
     * @param endpointClassName the endpoint class name
     * @param activationSpecConfigKey the key used to look up the
     *        {@code quarkus.ironjacamar.activation-spec.<key>.*} configuration, may be {@code null}
     */
    public ResourceEndpointBuildItem(String endpointClassName, String activationSpecConfigKey) {
        this(endpointClassName, activationSpecConfigKey, null);
    }

    /**
     * Constructor
     *
     * @param endpointClassName the endpoint class name
     * @param activationSpecConfigKey the key used to look up the
     *        {@code quarkus.ironjacamar.activation-spec.<key>.*} configuration, may be {@code null}
     * @param containerId the {@link ContainerCreatedBuildItem#identifier()} of the resource adapter this endpoint
     *        belongs to. Required when more than one resource adapter is configured
     */
    public ResourceEndpointBuildItem(String endpointClassName, String activationSpecConfigKey, String containerId) {
        this.endpointClassName = Objects.requireNonNull(endpointClassName, "endpointClassName cannot be null");
        this.activationSpecConfigKey = activationSpecConfigKey;
        this.containerId = containerId;
    }

    /**
     * Get the endpoint class name
     *
     * @return the endpoint class name
     */
    public String getEndpointClassName() {
        return endpointClassName;
    }

    /**
     * Get the activation spec configuration key
     *
     * @return the activation spec configuration key, may be {@code null}
     */
    public String getActivationSpecConfigKey() {
        return activationSpecConfigKey;
    }

    /**
     * Get the identifier of the resource adapter this endpoint belongs to
     *
     * @return the container identifier, may be {@code null}
     */
    public String getContainerId() {
        return containerId;
    }
}
