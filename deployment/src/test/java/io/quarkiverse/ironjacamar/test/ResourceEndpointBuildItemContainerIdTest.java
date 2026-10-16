package io.quarkiverse.ironjacamar.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.ironjacamar.Defaults;
import io.quarkiverse.ironjacamar.deployment.ContainerCreatedBuildItem;
import io.quarkiverse.ironjacamar.deployment.ResourceEndpointBuildItem;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
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
import io.smallrye.common.annotation.Identifier;

/**
 * Verifies that a contributed endpoint is only activated in the resource adapter identified by the container id of the
 * {@link ResourceEndpointBuildItem} when more than one resource adapter is configured.
 */
public class ResourceEndpointBuildItemContainerIdTest {

    private static final String OTHER = "other";

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
            .overrideConfigKey("quarkus.ironjacamar.other.ra.kind", "test")
            .overrideConfigKey("quarkus.ironjacamar.other.ra.cm.pool.config.idle-timeout-minutes", "0");

    /**
     * Contributes an endpoint targeting the only resource adapter that is not the default one, discovering the
     * deployed resource adapters through {@link ContainerCreatedBuildItem} the way another extension would.
     */
    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        String containerId = context.consumeMulti(ContainerCreatedBuildItem.class).stream()
                                .map(ContainerCreatedBuildItem::identifier)
                                .filter(identifier -> !Defaults.DEFAULT_RESOURCE_ADAPTER_NAME.equals(identifier))
                                .findFirst()
                                .orElseThrow();
                        context.produce(new ResourceEndpointBuildItem(ContributedResourceEndpoint.class.getName(),
                                "contributed", containerId));
                    }
                }).consumes(ContainerCreatedBuildItem.class)
                        .produces(ResourceEndpointBuildItem.class)
                        .build();
            }
        };
    }

    @Inject
    @Identifier(Defaults.DEFAULT_RESOURCE_ADAPTER_NAME)
    IronJacamarContainer defaultContainer;

    @Inject
    @Identifier(OTHER)
    IronJacamarContainer otherContainer;

    @Test
    public void shouldActivateContributedEndpointInTheIdentifiedContainerOnly() {
        RecordingResourceAdapterFactory otherFactory = (RecordingResourceAdapterFactory) otherContainer
                .getResourceAdapterFactory();
        assertThat(otherFactory.getActivations())
                .singleElement()
                .satisfies(activation -> {
                    assertThat(activation.containerId()).isEqualTo(OTHER);
                    assertThat(activation.endpointClass()).isEqualTo(ContributedResourceEndpoint.class);
                });

        RecordingResourceAdapterFactory defaultFactory = (RecordingResourceAdapterFactory) defaultContainer
                .getResourceAdapterFactory();
        assertThat(defaultFactory.getActivations())
                .filteredOn(activation -> Defaults.DEFAULT_RESOURCE_ADAPTER_NAME.equals(activation.containerId()))
                .isEmpty();
    }
}
