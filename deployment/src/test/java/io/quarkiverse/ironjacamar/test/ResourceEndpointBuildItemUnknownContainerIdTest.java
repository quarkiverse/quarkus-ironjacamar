package io.quarkiverse.ironjacamar.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.ironjacamar.deployment.ResourceEndpointBuildItem;
import io.quarkiverse.ironjacamar.test.adapter.ContributedResourceEndpoint;
import io.quarkiverse.ironjacamar.test.adapter.EndpointActivation;
import io.quarkiverse.ironjacamar.test.adapter.RecordingResourceAdapterFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestActivationSpec;
import io.quarkiverse.ironjacamar.test.adapter.TestConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestManagedConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceAdapter;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that the build fails when an endpoint is contributed for a resource adapter that is not configured,
 * instead of silently not activating it.
 */
public class ResourceEndpointBuildItemUnknownContainerIdTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(RecordingResourceAdapterFactory.class,
                            TestResourceAdapter.class,
                            TestManagedConnectionFactory.class,
                            TestActivationSpec.class,
                            TestConnectionFactory.class,
                            ContributedResourceEndpoint.class,
                            EndpointActivation.class))
            .addBuildChainCustomizer(buildCustomizer())
            .overrideConfigKey("quarkus.ironjacamar.ra.kind", "test")
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.idle-timeout-minutes", "0")
            // The exception is thrown in the augmentation class loader, hence the assertion on the message
            .assertException(throwable -> assertThat(throwable)
                    .hasMessageContaining("QIJ000022")
                    .hasMessageContaining(ContributedResourceEndpoint.class.getName())
                    .hasMessageContaining("unconfigured"));

    /**
     * Contributes an endpoint for a resource adapter that does not exist.
     */
    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new ResourceEndpointBuildItem(ContributedResourceEndpoint.class.getName(),
                                "contributed", "unconfigured"));
                    }
                }).produces(ResourceEndpointBuildItem.class)
                        .build();
            }
        };
    }

    @Test
    public void shouldFailAtBuildTime() {
        // The application is expected to fail to start, therefore this method is never invoked
    }
}
