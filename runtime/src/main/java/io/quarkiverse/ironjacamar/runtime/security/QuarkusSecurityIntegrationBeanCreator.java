package io.quarkiverse.ironjacamar.runtime.security;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

/**
 * Creates the {@link QuarkusSecurityIntegration} synthetic bean.
 */
public class QuarkusSecurityIntegrationBeanCreator implements BeanCreator<QuarkusSecurityIntegration> {

    @Override
    public QuarkusSecurityIntegration create(SyntheticCreationalContext<QuarkusSecurityIntegration> context) {
        return new QuarkusSecurityIntegration();
    }
}
