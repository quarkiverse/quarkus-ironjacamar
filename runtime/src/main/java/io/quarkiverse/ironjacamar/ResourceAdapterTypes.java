package io.quarkiverse.ironjacamar;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceAdapterTypes {

    /**
     * What types are provided by the ConnectionFactory returned by mcf.createConnectionFactory(cm).
     */
    Class<?>[] connectionFactoryTypes() default {};

}
