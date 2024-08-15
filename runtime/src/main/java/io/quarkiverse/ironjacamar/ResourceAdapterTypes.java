package io.quarkiverse.ironjacamar;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A Qualifier for resource adapters. This is used to identify the resource adapter to be used.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceAdapterTypes {

    /**
     * What types are provided by the ConnectionFactory returned by mcf.createConnectionFactory(cm).
     *
     * @return the types provided by the ConnectionFactory, never empty
     */
    Class<?>[] connectionFactoryTypes();

}
