package io.quarkiverse.ironjacamar;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a resource endpoint.
 */
@Target({ TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResourceEndpoint {
    ActivationSpec activationSpec() default @ActivationSpec(configKey = "<default>");

    @Target(ElementType.ANNOTATION_TYPE)
    @interface ActivationSpec {
        String configKey() default "<default>";

        ActivationConfigProperty[] properties() default {};

        @Target(ElementType.ANNOTATION_TYPE)
        @interface ActivationConfigProperty {
            String propertyName();

            String propertyValue();
        }
    }

}
