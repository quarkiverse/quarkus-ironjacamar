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

/**
 * The runtime configuration for IronJacamar
 */
@ConfigMapping(prefix = "quarkus.ironjacamar")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface IronJacamarRuntimeConfig {

    /**
     * The maximum amount of time the worker thread can be blocked.
     * If not specified it assumes the same value as defined by the <code>quarkus.vertx.max-worker-execute-time</code>
     * configuration.
     *
     * @return the maximum amount of time the worker thread can be blocked
     */
    @WithDefault("${quarkus.vertx.max-worker-execute-time:60}")
    Duration maxWorkerExecuteTime();

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
     * Activation Specs
     *
     * @return the activation specs
     */
    @WithName("activation-spec")
    ActivationSpecOuterNamedConfig activationSpecs();

    /**
     * The Resource Adapter configuration grouped by name
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
     * The Resource Adapter configuration
     */
    interface ResourceAdapterConfig {
        /**
         * The configuration for this resource adapter
         *
         * @return the configuration
         */
        Map<String, String> config();

        /**
         * The connection manager configuration for this resource adapter
         *
         * @return the connection manager configuration
         */
        ConnectionManagerConfig cm();
    }

    /**
     * The Connection Manager configuration
     */
    @ConfigGroup
    interface ConnectionManagerConfig {
        /**
         * The transaction support level for the Connection Manager
         * <p>
         * See the <a href=
         * "https://jakarta.ee/specifications/connectors/2.1/apidocs/jakarta.resource/jakarta/resource/spi/transactionsupport.transactionsupportlevel">TransactionSupportLevel
         * Javadoc</a> for more information
         *
         * @return the transaction support level
         */
        @WithDefault("XATransaction")
        TransactionSupport.TransactionSupportLevel transactionSupportLevel();

        /**
         * The number of times to retry the allocation of a connection
         *
         * @return the number of retries (default is 5 times)
         */
        @WithDefault("5")
        int allocationRetry();

        /**
         * The time to wait between retries of the allocation of a connection
         *
         * @return the time to wait (default is 1 second)
         */
        @WithDefault("1s")
        Duration allocationRetryWait();

        /**
         * The transaction timeout for the XAResource
         *
         * @return the transaction timeout (default is 120 seconds)
         */
        @WithDefault("120s")
        Duration xaResourceTimeout();

        /**
         * The flush strategy for the Connection Manager
         *
         * @return the flush strategy (default is failing-connection-only)
         */
        @WithDefault("failing-connection-only")
        FlushStrategy flushStrategy();

        /**
         * Whether the connection manager is sharable
         *
         * @return whether the connection manager is sharable (default is true)
         */
        @WithDefault("true")
        boolean sharable();

        /**
         * Whether the connection manager should enlist connections
         *
         * @return whether the connection manager should enlist connections (default is true)
         */
        @WithDefault("true")
        boolean enlistment();

        /**
         * Whether the connection manager should be connectable
         *
         * @return whether the connection manager should be connectable (default is false)
         */
        @WithDefault("false")
        boolean connectable();

        /**
         * Whether the connection manager should track connections
         *
         * @return whether the connection manager should track connections (default is false)
         */
        Optional<Boolean> tracking();

        /**
         * Whether the connection manager should use CCM
         *
         * @return whether the connection manager should use CCM (default is true)
         */
        @WithDefault("true")
        boolean useCcm();

        /**
         * Whether the connection manager should use interleaving
         *
         * @return whether the connection manager should use interleaving (default is false)
         */
        @WithDefault("false")
        boolean interleaving();

        /**
         * Whether the connection manager should use same RM override
         *
         * @return whether the connection manager should use same RM override
         */
        @WithName("is-same-rm-override")
        Optional<Boolean> isSameRMOverride();

        /**
         * Whether the connection manager should wrap the XAResource
         *
         * @return whether the connection manager should wrap the XAResource (default is true)
         */
        @WithDefault("true")
        @WithName("wrap-xa-resource")
        boolean wrapXAResource();

        /**
         * Whether the connection manager should pad the XID
         *
         * @return whether the connection manager should pad the XID (default is false)
         */
        @WithDefault("false")
        boolean padXid();

        /**
         * The recovery configuration for the Connection Manager
         *
         * @return the recovery configuration
         */
        RecoveryConfig recovery();

        /**
         * The recovery configuration for the Connection Manager
         */
        @ConfigGroup
        interface RecoveryConfig {
            /**
             * The recovery username for the Connection Manager
             *
             * @return the recovery username
             */
            Optional<String> username();

            /**
             * The recovery password for the Connection Manager
             *
             * @return the recovery password
             */
            Optional<String> password();

            /**
             * The recovery security domain for the Connection Manager
             *
             * @return the recovery security domain
             */
            Optional<String> securityDomain();
        }

        /**
         * The pool configuration for the Connection Manager
         *
         * @return the pool configuration
         */
        PoolConfig pool();

        /**
         * The pool configuration for the Connection Manager
         */
        @ConfigGroup
        interface PoolConfig {

            /**
             * The pool strategy
             *
             * @return the pool strategy (default is pool-by-cri)
             */
            @WithDefault("pool-by-cri")
            PoolStrategy strategy();

            /**
             * The pool configuration
             *
             * @return the pool configuration
             */
            PoolConfigurationConfig config();

            /**
             * Whether the pool is sharable
             *
             * @return whether the pool is sharable (default is true)
             */
            @WithDefault("true")
            boolean sharable();

            /**
             * Should the pool be created without a separate pool for non-transactional connections?
             *
             * @return whether the pool should be created without a separate pool for non-transactional connections (default is
             *         false)
             */
            @WithDefault("false")
            boolean noTxSeparatePool();

            /**
             * The pool configuration
             */
            interface PoolConfigurationConfig {
                /**
                 * Minimum size of the pool
                 *
                 * @return the minimum size of the pool (default is 0)
                 */
                @WithDefault("0")
                int minSize();

                /**
                 * Initial size of the pool
                 *
                 * @return the initial size of the pool
                 */
                OptionalInt initialSize();

                /**
                 * Maximum size of the pool
                 *
                 * @return the maximum size of the pool (default is 20)
                 */
                @WithDefault("20")
                int maxSize();

                /**
                 * Blocking timeout
                 *
                 * @return the blocking timeout (default is 30000ms)
                 */
                @WithDefault("30000ms")
                Duration blockingTimeout();

                /**
                 * Idle timeout period. Default 30 mins
                 *
                 * @return the idle timeout period (default is 30 mins)
                 */
                @WithDefault("30m")
                Duration idleTimeoutMinutes();

                /**
                 * Validate on match validation
                 *
                 * @return whether to validate on match (default is false)
                 */
                @WithDefault("false")
                boolean validateOnMatch();

                /**
                 * Background validation
                 *
                 * @return whether to background validate (default is false)
                 */
                @WithDefault("false")
                boolean backgroundValidation();

                /**
                 * Background validation - millis
                 *
                 * @return the background validation period
                 */
                Optional<Duration> backgroundValidationMillis();

                /**
                 * Prefill pool
                 *
                 * @return whether to prefill the pool (default is false)
                 */
                @WithDefault("false")
                boolean prefill();

                /**
                 * Strict minimum, default false
                 *
                 * @return whether to use strict minimum (default is false)
                 */
                @WithDefault("false")
                boolean strictMin();

                /**
                 * Do we want to immediately break when a connection cannot be matched and
                 * not evaluate the rest of the pool?
                 *
                 * @return whether to use fast fail (default is false)
                 */
                @WithDefault("false")
                boolean useFastFail();

                /**
                 * Fairness of semaphore permits, default true
                 *
                 * @return whether to use fair semaphore permits (default is true)
                 */
                @WithDefault("true")
                boolean fair();

                /**
                 * Convert this configuration to a {@link PoolConfiguration}
                 *
                 * @return the {@link PoolConfiguration}
                 */
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

    /**
     * The Activation Spec configuration grouped by name
     */
    @ConfigGroup
    interface ActivationSpecOuterNamedConfig {

        /**
         * The Activation Spec configuration.
         *
         * @return the Activation Spec configuration
         */
        @ConfigDocMapKey("activation-spec-name")
        @WithParentName
        @WithDefaults
        @WithUnnamedKey(Defaults.DEFAULT_ACTIVATION_SPEC_NAME)
        Map<String, ActivationSpecConfig> map();
    }

    /**
     * The Activation Spec configuration
     */
    interface ActivationSpecConfig {
        /**
         * The configuration for this resource adapter
         *
         * @return the configuration
         */
        Map<String, String> config();
    }
}
