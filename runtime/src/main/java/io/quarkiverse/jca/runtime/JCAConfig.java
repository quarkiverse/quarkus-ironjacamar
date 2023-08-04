package io.quarkiverse.jca.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.jca")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface JCAConfig {

    /**
     * The resource adapters to deploy.
     */
    @ConfigDocSection
    @ConfigDocMapKey("resource-adapter-name")
    @WithParentName
    @WithDefaults
    Map<String, ResourceAdapterConfig> namedResourceAdapters();

    @ConfigGroup
    interface ResourceAdapterConfig {

        /**
         * The configuration properties for the resource adapter.
         */
        Map<String, String> getConfigProperties();

        /**
         * The connection definitions.
         */
        List<ConnectionDefinitionConfig> connectionDefinitions();

        /**
         * The admin objects.
         */
        List<AdminObjectConfig> adminObjects();
    }

    @ConfigGroup
    interface ConnectionDefinitionConfig {
        /**
         * The class name of the connection factory.
         */
        String getClassName();
    }

    @ConfigGroup
    interface AdminObjectConfig {
        /**
         * The class name of the admin object.
         */
        String getClassName();

        /**
         * The configuration properties for the admin object.
         */
        Map<String, String> getConfigProperties();
    }
}
