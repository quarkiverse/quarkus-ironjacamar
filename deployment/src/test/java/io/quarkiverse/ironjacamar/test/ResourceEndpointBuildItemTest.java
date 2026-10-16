package io.quarkiverse.ironjacamar.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.ironjacamar.ResourceEndpoint;
import io.quarkiverse.ironjacamar.deployment.ResourceEndpointBuildItem;
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
 * Verifies that endpoints contributed through a {@link ResourceEndpointBuildItem} are activated, even though they are
 * not annotated with {@link ResourceEndpoint}, and that they honor the activation spec configuration key they were
 * registered with.
 */
public class ResourceEndpointBuildItemTest {

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
            .overrideConfigKey("quarkus.ironjacamar.activation-spec.contributed.config.destination", "ContributedQueue")
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.idle-timeout-minutes", "0");

    /**
     * Simulates another extension contributing an endpoint it generated at build time.
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
            }
        };
    }

    @Inject
    IronJacamarContainer ironJacamarContainer;

    @Test
    public void shouldActivateContributedEndpoint() {
        assertThat(activations())
                .extracting(EndpointActivation::endpointClass)
                .containsExactlyInAnyOrder(TestResourceEndpoint.class, ContributedResourceEndpoint.class);
    }

    @Test
    public void shouldApplyActivationSpecConfigOfContributedEndpoint() {
        assertThat(activations())
                .filteredOn(activation -> activation.endpointClass() == ContributedResourceEndpoint.class)
                .singleElement()
                .satisfies(activation -> assertThat(activation.config()).containsEntry("destination", "ContributedQueue"));
    }

    private List<EndpointActivation> activations() {
        return ((RecordingResourceAdapterFactory) ironJacamarContainer.getResourceAdapterFactory()).getActivations();
    }
}
