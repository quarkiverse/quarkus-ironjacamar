package io.quarkiverse.ironjacamar;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
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
    /**
     * @return The ActivationSpec config key
     */
    String activationSpecConfigKey() default Defaults.DEFAULT_ACTIVATION_SPEC_NAME;

}
