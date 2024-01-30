package io.quarkiverse.ironjacamar.runtime.security;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.jca.core.spi.security.Callback;
import org.jboss.jca.core.spi.security.SecurityContext;
import org.jboss.jca.core.spi.security.SecurityIntegration;

/**
 * Default {@link SecurityIntegration} implementation for Quarkus
 */
public class QuarkusSecurityIntegration implements SecurityIntegration {

    private SecurityContext securityContext;

    /**
     * Constructor
     */
    public QuarkusSecurityIntegration() {
    }

    /**
     * @return a new {@link QuarkusSecurityContext} instance
     */
    @Override
    public SecurityContext createSecurityContext(String sd) {
        return new QuarkusSecurityContext();
    }

    /**
     * @return the {@link SecurityContext}
     */
    @Override
    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    /**
     * Set the {@link SecurityContext}
     *
     * @param context The context
     */
    @Override
    public void setSecurityContext(SecurityContext context) {
        this.securityContext = context;
    }

    /**
     * @return a new {@link QuarkusCallbackHandler} instance
     */
    @Override
    public CallbackHandler createCallbackHandler() {
        return new QuarkusCallbackHandler(null);
    }

    /**
     * @return a new {@link QuarkusCallbackHandler} instance
     */
    @Override
    public CallbackHandler createCallbackHandler(Callback callback) {
        return new QuarkusCallbackHandler(callback);
    }
}
