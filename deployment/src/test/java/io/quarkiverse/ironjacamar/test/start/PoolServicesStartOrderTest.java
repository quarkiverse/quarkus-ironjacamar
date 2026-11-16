package io.quarkiverse.ironjacamar.test.start;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.ironjacamar.test.adapter.RecordingResourceAdapterFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestActivationSpec;
import io.quarkiverse.ironjacamar.test.adapter.TestConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestManagedConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceAdapter;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.core.deployment.service.impl.ServiceMetadataBuildItem;
import io.quarkus.core.deployment.service.impl.ServiceRegistrarImpl;
import io.quarkus.core.deployment.service.impl.ServiceValueRetentionBuildItem;
import io.quarkus.core.deployment.service.impl.StaticServiceMetadataBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The pool services (IdleRemover, ConnectionValidator) run their background task on an executor they get from the
 * Quarkus runtime. The runtime service graph is drained in FIFO order and the phases are not barriers: a service
 * starts as soon as the services it requires are started, so a service of the {@link Phase#APPLICATION} phase may
 * well start before a service of the {@link Phase#INFRASTRUCTURE} phase.
 * <p>
 * The {@link ScheduledExecutorService} service of the Quarkus thread pool belongs to the infrastructure phase and
 * the pool services must require it: the executor of the {@code ManagedExecutor} bean is the one of the thread pool
 * at the moment the bean is created, and it is {@code null} as long as the thread pool is not started.
 * <p>
 * This application starts both pool services and delays the thread pool, so that the pool services are started
 * first unless they require it.
 */
public class PoolServicesStartOrderTest {

    private static final String DELAY_SERVICE_NAME = "io.quarkiverse.ironjacamar.test.delay-thread-pool";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(
                            RecordingResourceAdapterFactory.class,
                            TestResourceAdapter.class,
                            TestManagedConnectionFactory.class,
                            TestConnectionFactory.class,
                            TestActivationSpec.class))
            .addBuildChainCustomizer(delayTheThreadPool())
            .overrideConfigKey("quarkus.ironjacamar.ra.kind", "test")
            // start the IdleRemover
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.idle-timeout-minutes", "30")
            // start the ConnectionValidator
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.background-validation", "true")
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.background-validation-millis", "60000");

    /**
     * Register a service the Quarkus thread pool has to wait for, which itself waits for the
     * {@link BeanContainer}: the pool services are queued in the service graph as soon as the bean container is
     * started, hence before the thread pool can be queued.
     */
    static Consumer<BuildChainBuilder> delayTheThreadPool() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        ServiceRegistrar serviceRegistrar = new ServiceRegistrarImpl(
                                context::produce,
                                context::produce,
                                context::produce,
                                context::produce,
                                context::produce,
                                DELAY_SERVICE_NAME);
                        serviceRegistrar
                                .forService(DELAY_SERVICE_NAME)
                                .atPhase(Phase.INFRASTRUCTURE)
                                .require(BeanContainer.class)
                                .before(ScheduledExecutorService.class)
                                .onStart((startContext, beanContainer) -> {
                                });
                    }
                })
                        .produces(MainBytecodeRecorderBuildItem.class)
                        .produces(StaticBytecodeRecorderBuildItem.class)
                        .produces(ServiceMetadataBuildItem.class)
                        .produces(StaticServiceMetadataBuildItem.class)
                        .produces(ServiceValueRetentionBuildItem.class)
                        .build();
            }
        };
    }

    @Inject
    ManagedExecutor managedExecutor;

    /**
     * The application fails to start when a pool service creates the {@code ManagedExecutor} bean before the
     * Quarkus thread pool is started: {@code IdleRemover.start()} throws a {@link NullPointerException}.
     */
    @Test
    void poolServicesStartAfterTheThreadPool() throws Exception {
        CompletableFuture<String> executed = new CompletableFuture<>();
        managedExecutor.execute(() -> executed.complete(Thread.currentThread().getName()));
        assertThat(executed.get(10, TimeUnit.SECONDS)).isNotBlank();
    }
}
