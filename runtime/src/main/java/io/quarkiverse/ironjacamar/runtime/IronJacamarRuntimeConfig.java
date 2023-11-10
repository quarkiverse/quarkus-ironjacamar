package io.quarkiverse.ironjacamar.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import jakarta.resource.spi.TransactionSupport;

import org.jboss.jca.common.api.metadata.common.FlushStrategy;
import org.jboss.jca.core.api.connectionmanager.pool.PoolConfiguration;
import org.jboss.jca.core.connectionmanager.pool.api.PoolStrategy;

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

        /**
         * The connection manager configuration for this resource adapter
         */
        ConnectionManagerConfig cm();
    }

    @ConfigGroup
    interface ConnectionManagerConfig {
        /**
         * The transaction support level for the Connection Manager
         * <p>
         * See the <a href=
         * "https://jakarta.ee/specifications/connectors/2.1/apidocs/jakarta.resource/jakarta/resource/spi/transactionsupport.transactionsupportlevel">TransactionSupportLevel
         * Javadoc</a> for more information
         */
        @WithDefault("XATransaction")
        TransactionSupport.TransactionSupportLevel transactionSupportLevel();

        /**
         * The number of times to retry the allocation of a connection
         */
        @WithDefault("5")
        int allocationRetry();

        /**
         * The time to wait between retries of the allocation of a connection
         */
        @WithDefault("1s")
        Duration allocationRetryWait();

        /**
         * The transaction timeout for the XAResource
         */
        @WithDefault("120s")
        Duration xaResourceTimeout();

        /**
         * The flush strategy for the Connection Manager
         */
        @WithDefault("failing-connection-only")
        FlushStrategy flushStrategy();

        /**
         * Whether the connection manager is sharable
         */
        @WithDefault("true")
        boolean sharable();

        /**
         * Whether the connection manager should enlist connections
         */
        @WithDefault("true")
        boolean enlistment();

        /**
         * Whether the connection manager should be connectable
         */
        @WithDefault("false")
        boolean connectable();

        /**
         * Whether the connection manager should track connections
         */
        Optional<Boolean> tracking();

        /**
         * Whether the connection manager should use CCM
         */
        @WithDefault("true")
        boolean useCcm();

        /**
         * Whether the connection manager should use interleaving
         */
        @WithDefault("false")
        boolean interleaving();

        /**
         * Whether the connection manager should use same RM override
         */
        @WithName("is-same-rm-override")
        Optional<Boolean> isSameRMOverride();

        /**
         * Whether the connection manager should wrap the XAResource
         */
        @WithDefault("true")
        @WithName("wrap-xa-resource")
        boolean wrapXAResource();

        /**
         * Whether the connection manager should pad the XID
         */
        @WithDefault("false")
        boolean padXid();

        /**
         * The recovery configuration for the Connection Manager
         */
        RecoveryConfig recovery();

        @ConfigGroup
        interface RecoveryConfig {
            /**
             * The recovery username for the Connection Manager
             */
            Optional<String> username();

            /**
             * The recovery password for the Connection Manager
             */
            Optional<String> password();

            /**
             * The recovery security domain for the Connection Manager
             */
            Optional<String> securityDomain();
        }

        /**
         * The pool configuration for the Connection Manager
         */
        PoolConfig pool();

        @ConfigGroup
        interface PoolConfig {

            /**
             * The pool strategy
             */
            @WithDefault("pool-by-cri")
            PoolStrategy strategy();

            /**
             * The pool configuration
             */
            PoolConfigurationConfig config();

            /**
             * Whether the pool is sharable
             */
            @WithDefault("true")
            boolean sharable();

            /**
             * Should the pool be created without a separate pool for non-transactional connections?
             */
            @WithDefault("false")
            boolean noTxSeparatePool();

            interface PoolConfigurationConfig {
                /**
                 * Minimum size of the pool
                 */
                @WithDefault("0")
                int minSize();

                /**
                 * Initial size of the pool
                 */
                OptionalInt initialSize();

                /**
                 * Maximum size of the pool
                 */
                @WithDefault("20")
                int maxSize();

                /**
                 * Blocking timeout
                 */
                @WithDefault("30000ms")
                Duration blockingTimeout();

                /**
                 * Idle timeout period. Default 30 mins
                 */
                @WithDefault("30m")
                Duration idleTimeoutMinutes();

                /**
                 * Validate on match validation
                 */
                @WithDefault("false")
                boolean validateOnMatch();

                /**
                 * Background validation
                 */
                @WithDefault("false")
                boolean backgroundValidation();

                /**
                 * Background validation - millis
                 */
                Optional<Duration> backgroundValidationMillis();

                /**
                 * Prefill pool
                 */
                @WithDefault("false")
                boolean prefill();

                /**
                 * Strict minimum, default false
                 */
                @WithDefault("false")
                boolean strictMin();

                /**
                 * Do we want to immediately break when a connection cannot be matched and
                 * not evaluate the rest of the pool?
                 */
                @WithDefault("false")
                boolean useFastFail();

                /**
                 * Fairness of semaphore permits, default true
                 */
                @WithDefault("true")
                boolean fair();

                default PoolConfiguration toPoolConfiguration() {
                    PoolConfiguration poolConfiguration = new PoolConfiguration();
                    poolConfiguration.setMinSize(minSize());
                    initialSize().ifPresent(poolConfiguration::setInitialSize);
                    poolConfiguration.setMaxSize(maxSize());
                    poolConfiguration.setBlockingTimeout(blockingTimeout().toMillis());
                    poolConfiguration.setIdleTimeoutMinutes(idleTimeoutMinutes().toMinutesPart());
                    poolConfiguration.setValidateOnMatch(validateOnMatch());
                    poolConfiguration.setBackgroundValidation(backgroundValidation());
                    backgroundValidationMillis().ifPresent(d -> poolConfiguration.setBackgroundValidationMillis(d.toMillis()));
                    poolConfiguration.setPrefill(prefill());
                    poolConfiguration.setStrictMin(strictMin());
                    poolConfiguration.setUseFastFail(useFastFail());
                    poolConfiguration.setFair(fair());
                    return poolConfiguration;
                }
            }
        }
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
