package io.quarkiverse.ironjacamar;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * A Qualifier for resource adapters. This is used to identify the resource adapter to be used.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceAdapterKind {
    /**
     * The kind of resource adapter.
     */
    String value();

    // Literal for this CDI qualifier
    class Literal extends AnnotationLiteral<ResourceAdapterKind> implements ResourceAdapterKind {
        private final String value;

        public Literal(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}
