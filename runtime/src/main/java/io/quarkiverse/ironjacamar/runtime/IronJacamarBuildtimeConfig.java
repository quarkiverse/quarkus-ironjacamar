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

/**
 * The IronJacamar build time configuration.
 */
@ConfigMapping(prefix = "quarkus.ironjacamar")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface IronJacamarBuildtimeConfig {

    /**
     * Whether IronJacamar (pool) metrics are published in case a metrics extension is present.
     * <p>
     * This is a global setting and is not specific to a resource adapter.
     * </p>
     *
     * @return whether IronJacamar (pool) metrics are enabled
     */
    @WithName("metrics.enabled")
    @WithDefault("false")
    boolean metricsEnabled();

    /**
     * Resource Adapters
     *
     * @return the resource adapters
     */
    @ConfigDocMapKey("resource-adapter-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(Defaults.DEFAULT_RESOURCE_ADAPTER_NAME)
    Map<String, ResourceAdapterOuterNamedConfig> resourceAdapters();

    /**
     * The Resource adapter configuration.
     */
    @ConfigGroup
    interface ResourceAdapterOuterNamedConfig {

        /**
         * The Resource adapter configuration.
         *
         * @return the resource adapter configuration
         */
        ResourceAdapterConfig ra();
    }

    /**
     * The Resource adapter configuration.
     */
    interface ResourceAdapterConfig {
        /**
         * The kind of resource adapter.
         *
         * @return the kind of resource adapter
         */
        Optional<String> kind();

        /**
         * The connection manager configuration for this resource adapter
         *
         * @return the connection manager configuration
         */
        ConnectionManagerBuildConfig cm();
    }

    /**
     * The connection manager configuration for this resource adapter
     */
    @ConfigGroup
    interface ConnectionManagerBuildConfig {
        /**
         * The pool configuration for this resource adapter
         *
         * @return the pool configuration
         */
        PoolBuildConfig pool();
    }

    /**
     * The pool configuration for this resource adapter
     */
    @ConfigGroup
    interface PoolBuildConfig {
        /**
         * Enable pool metrics collection. If unspecified, collecting metrics will be enabled by default if
         * a metrics extension is active.
         *
         * @return whether pool metrics collection is enabled
         */
        Optional<Boolean> enableMetrics();
    }
}
