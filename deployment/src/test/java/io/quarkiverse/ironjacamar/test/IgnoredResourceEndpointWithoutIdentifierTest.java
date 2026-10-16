package io.quarkiverse.ironjacamar.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.ironjacamar.Defaults;
import io.quarkiverse.ironjacamar.deployment.ResourceEndpointIgnoredBuildItem;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
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
import io.smallrye.common.annotation.Identifier;

/**
 * Verifies that an ignored endpoint does not need to tell which resource adapter it belongs to, even when more than
 * one resource adapter is configured: the extension managing it is free to deliver the messages the way it wants, so
 * the usual {@link Identifier} requirement (QIJ000005) does not apply.
 */
public class IgnoredResourceEndpointWithoutIdentifierTest {

    private static final String OTHER = "other";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(RecordingResourceAdapterFactory.class,
                            TestResourceAdapter.class,
                            TestManagedConnectionFactory.class,
                            TestActivationSpec.class,
                            TestConnectionFactory.class,
                            TestResourceEndpoint.class,
                            EndpointActivation.class))
            .addBuildChainCustomizer(buildCustomizer())
            .overrideConfigKey("quarkus.ironjacamar.ra.kind", "test")
            .overrideConfigKey("quarkus.ironjacamar.ra.cm.pool.config.idle-timeout-minutes", "0")
            .overrideConfigKey("quarkus.ironjacamar.other.ra.kind", "test")
            .overrideConfigKey("quarkus.ironjacamar.other.ra.cm.pool.config.idle-timeout-minutes", "0");

    /**
     * Simulates another extension taking over {@link TestResourceEndpoint}, which carries no {@link Identifier}.
     */
    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new ResourceEndpointIgnoredBuildItem(
                                classInfo -> classInfo.name().toString().equals(TestResourceEndpoint.class.getName())));
                    }
                }).produces(ResourceEndpointIgnoredBuildItem.class)
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
    public void shouldStartWithoutActivatingTheIgnoredEndpoint() {
        assertThat(activationsOf(defaultContainer)).isEmpty();
        assertThat(activationsOf(otherContainer)).isEmpty();
    }

    private static List<EndpointActivation> activationsOf(IronJacamarContainer container) {
        return ((RecordingResourceAdapterFactory) container.getResourceAdapterFactory()).getActivations();
    }
}
