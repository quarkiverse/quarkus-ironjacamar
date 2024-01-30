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
     *
     * @return the kind of resource adapter
     */
    String value();

    /**
     * Literal for {@link ResourceAdapterKind}.
     */
    class Literal extends AnnotationLiteral<ResourceAdapterKind> implements ResourceAdapterKind {

        /**
         * The value for the resource adapter kind.
         */
        private final String value;

        /**
         * Creates a new literal.
         *
         * @param value the value for the resource adapter kind
         * @return the literal
         */
        public static Literal of(String value) {
            return new Literal(value);
        }

        /**
         * Constructor
         *
         * @param value the value for the resource adapter kind
         */
        public Literal(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}
