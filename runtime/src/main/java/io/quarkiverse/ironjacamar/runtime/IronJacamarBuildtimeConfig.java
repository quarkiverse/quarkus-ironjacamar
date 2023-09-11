package io.quarkiverse.ironjacamar.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkiverse.ironjacamar.Defaults;
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
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface IronJacamarBuildtimeConfig {

    /**
     * Whether IronJacamar (pool) metrics are published in case a metrics extension is present.
     * <p>
     * This is a global setting and is not specific to a resource adapter.
     * <p>
     */
    @WithName("metrics.enabled")
    @WithDefault("false")
    boolean metricsEnabled();

    /**
     * Resource Adapters
     */
    @ConfigDocMapKey("resource-adapter-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(Defaults.DEFAULT_RESOURCE_ADAPTER_NAME)
    Map<String, ResourceAdapterOuterNamedConfig> resourceAdapters();

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
         * The connection manager configuration for this resource adapter
         */
        ConnectionManagerBuildConfig cm();
    }

    @ConfigGroup
    interface ConnectionManagerBuildConfig {
        /**
         * The pool configuration for this resource adapter
         */
        PoolBuildConfig pool();
    }

    @ConfigGroup
    interface PoolBuildConfig {
        /**
         * Enable pool metrics collection. If unspecified, collecting metrics will be enabled by default if
         * a metrics extension is active.
         */
        Optional<Boolean> enableMetrics();
    }
}
