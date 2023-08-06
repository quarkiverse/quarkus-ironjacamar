package io.quarkiverse.jca.runtime.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ActivationConfigProperty {

    String name();

    String value();

}
