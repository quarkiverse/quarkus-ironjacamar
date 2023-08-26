package io.quarkiverse.ironjacamar.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.ironjacamar")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface IronJacamarConfig {

    String DEFAULT_RESOURCE_ADAPTER_NAME = "<default>";

    String DEFAULT_ACTIVATION_SPEC_NAME = "<default>";

    /**
     * Whether IronJacamar is enabled.
     *
     * @return {@code true} if IronJacamar is enabled, {@code false} otherwise
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Resource Adapters
     */
    @ConfigDocMapKey("resource-adapter-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(DEFAULT_RESOURCE_ADAPTER_NAME)
    Map<String, ResourceAdapterOuterNamedConfig> resourceAdapters();

    /**
     * Activation Specs
     */
    @WithName("activation-spec")
    ActivationSpecOuterNamedConfig activationSpecs();

    @ConfigGroup
    interface ResourceAdapterOuterNamedConfig {

        /**
         * The Resource adapter configuration.
         */
        ResourceAdapterConfig ra();
    }

    interface ResourceAdapterConfig {
        /**
         * The kind of resource adapter.
         */
        Optional<String> kind();

        /**
         * The configuration for this resource adapter
         */
        Map<String, String> config();
    }

    @ConfigGroup
    interface ActivationSpecOuterNamedConfig {

        /**
         * The Activation Spec configuration.
         */
        @ConfigDocMapKey("activation-spec-name")
        @WithParentName
        @WithDefaults
        @WithUnnamedKey(DEFAULT_RESOURCE_ADAPTER_NAME)
        Map<String, ActivationSpecConfig> map();
    }

    interface ActivationSpecConfig {
        /**
         * The configuration for this resource adapter
         */
        Map<String, String> config();
    }
}
