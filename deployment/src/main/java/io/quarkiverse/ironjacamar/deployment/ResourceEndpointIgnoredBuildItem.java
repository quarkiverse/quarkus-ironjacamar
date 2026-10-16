package io.quarkiverse.ironjacamar.deployment;

import java.util.Objects;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that excludes {@link io.quarkiverse.ironjacamar.ResourceEndpoint} annotated classes from the automatic
 * endpoint activation performed by this extension.
 * <p>
 * Extensions produce this build item when they take over the activation of an endpoint themselves, usually by
 * producing a {@link ResourceEndpointBuildItem} for a class they generated. Ignored classes are still registered as
 * beans, only their activation is skipped.
 * <p>
 * The predicate is evaluated against every class annotated with {@link io.quarkiverse.ironjacamar.ResourceEndpoint},
 * which allows matching on whatever the producing extension cares about, e.g. the class name, an annotation on the
 * class or one of its methods. It is also evaluated against the endpoints contributed through a
 * {@link ResourceEndpointBuildItem}, as long as their class is available in the index (endpoints generated at build
 * time usually are not).
 *
 * <pre>{@code
 * private static final DotName MY_LISTENER = DotName.createSimple("com.acme.MyListener");
 *
 * // @BuildStep
 * void ignoreEndpointsHandledByThisExtension(BuildProducer<ResourceEndpointIgnoredBuildItem> ignoredEndpoints) {
 *     // Endpoints with a @MyListener method are activated by this extension instead
 *     ignoredEndpoints.produce(new ResourceEndpointIgnoredBuildItem(
 *             classInfo -> classInfo.hasAnnotation(MY_LISTENER)));
 * }
 * }</pre>
 *
 * An ignored endpoint does not have to carry an {@link io.smallrye.common.annotation.Identifier} annotation, even
 * when several resource adapters are deployed, since it is no longer this extension that decides where it is
 * activated.
 */
public final class ResourceEndpointIgnoredBuildItem extends MultiBuildItem {

    private final Predicate<ClassInfo> predicate;

    /**
     * Constructor
     *
     * @param predicate returns {@code true} for the endpoint classes that must not be activated
     */
    public ResourceEndpointIgnoredBuildItem(Predicate<ClassInfo> predicate) {
        this.predicate = Objects.requireNonNull(predicate, "predicate cannot be null");
    }

    /**
     * Test if the given endpoint class must be ignored
     *
     * @param classInfo the endpoint class
     * @return {@code true} if the endpoint must not be activated
     */
    public boolean test(ClassInfo classInfo) {
        return predicate.test(classInfo);
    }
}
