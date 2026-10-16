package io.quarkiverse.ironjacamar.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.ironjacamar.deployment.ResourceEndpointBuildItem;
import io.quarkiverse.ironjacamar.deployment.ResourceEndpointIgnoredBuildItem;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.quarkiverse.ironjacamar.test.adapter.ContributedResourceEndpoint;
import io.quarkiverse.ironjacamar.test.adapter.EndpointActivation;
import io.quarkiverse.ironjacamar.test.adapter.RecordingResourceAdapterFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestActivationSpec;
import io.quarkiverse.ironjacamar.test.adapter.TestConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestManagedConnectionFactory;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceAdapter;
import io.quarkiverse.ironjacamar.test.adapter.TestResourceEndpoint;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that a {@link ResourceEndpointIgnoredBuildItem} also suppresses endpoints contributed through a
 * {@link ResourceEndpointBuildItem}, as long as their class is available in the index.
 */
public class ContributedResourceEndpointIgnoredTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(RecordingResourceAdapterFactory.class,
                            TestResourceAdapter.class,
                            TestManagedConnectionFactory.class,
                            TestActivationSpec.class,
                            TestConnectionFactory.class,
                            TestResourceEndpoint.class,
                            ContributedResourceEndpoint.class,
                            EndpointActivation.class))
            .addBuildChainCustomizer(buildCustomizer())
            .overrideConfigKey("quarkus.ironjacamar.ra.kind", "test")
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.idle-timeout-minutes", "0");

    /**
     * Contributes an endpoint and ignores it right away, as a second extension would.
     */
    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new ResourceEndpointBuildItem(ContributedResourceEndpoint.class.getName(),
                                "contributed"));
                    }
                }).produces(ResourceEndpointBuildItem.class)
                        .build();
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new ResourceEndpointIgnoredBuildItem(
                                classInfo -> classInfo.name().toString()
                                        .equals(ContributedResourceEndpoint.class.getName())));
                    }
                }).produces(ResourceEndpointIgnoredBuildItem.class)
                        .build();
            }
        };
    }

    @Inject
    IronJacamarContainer ironJacamarContainer;

    @Test
    public void shouldNotActivateIgnoredContributedEndpoint() {
        RecordingResourceAdapterFactory factory = (RecordingResourceAdapterFactory) ironJacamarContainer
                .getResourceAdapterFactory();
        assertThat(factory.getActivations())
                .extracting(EndpointActivation::endpointClass)
                .containsExactly(TestResourceEndpoint.class);
    }
}
