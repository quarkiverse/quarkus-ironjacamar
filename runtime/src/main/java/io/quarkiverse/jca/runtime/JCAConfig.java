package io.quarkiverse.jca.runtime;

import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperties;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigProperties(prefix = "quarkus.jca")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class JCAConfig {

    /**
     * The resource adapters to deploy.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, ResourceAdapterConfig> resourceAdapterConfigs;

    @ConfigGroup
    public static class ResourceAdapterConfig {

        /**
         * The configuration properties for the resource adapter.
         */
        public Map<String, String> configProperties;

        //        /**
        //         * The connection definitions.
        //         */
        //        public Optional<List<ConnectionDefinitionConfig>> connectionDefinitions;
        //
        //        /**
        //         * The admin objects.
        //         */
        //        public Optional<List<AdminObjectConfig>> adminObjects;
    }

    //
    @ConfigGroup
    public static class ConnectionDefinitionConfig {
        /**
         * The class name of the connection factory.
         */
        public String className;
    }

    @ConfigGroup
    public static class AdminObjectConfig {
        /**
         * The class name of the admin object.
         */
        public String className;

        /**
         * The configuration properties for the admin object.
         */
        public Map<String, String> configProperties;
    }
}
