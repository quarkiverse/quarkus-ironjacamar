package io.quarkiverse.ironjacamar.runtime;

import java.util.Map;

import io.quarkiverse.ironjacamar.Defaults;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.ironjacamar")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface IronJacamarRuntimeConfig {

    /**
     * Resource Adapters
     */
    @ConfigDocMapKey("resource-adapter-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(Defaults.DEFAULT_RESOURCE_ADAPTER_NAME)
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
        @WithUnnamedKey(Defaults.DEFAULT_ACTIVATION_SPEC_NAME)
        Map<String, ActivationSpecConfig> map();
    }

    interface ActivationSpecConfig {
        /**
         * The configuration for this resource adapter
         */
        Map<String, String> config();
    }
}
