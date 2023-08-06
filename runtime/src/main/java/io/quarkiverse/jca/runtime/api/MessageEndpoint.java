package io.quarkiverse.jca.runtime.api;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageEndpoint {
    /**
     * Activation config properties.
     */
    ActivationConfigProperty[] activationConfig() default {};
}
