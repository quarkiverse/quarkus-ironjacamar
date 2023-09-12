package io.quarkiverse.ironjacamar.runtime.security;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.jca.core.spi.security.Callback;
import org.jboss.jca.core.spi.security.SecurityContext;
import org.jboss.jca.core.spi.security.SecurityIntegration;

public class QuarkusSecurityIntegration implements SecurityIntegration {

    private SecurityContext securityContext;

    @Override
    public SecurityContext createSecurityContext(String sd) throws Exception {
        return new QuarkusSecurityContext();
    }

    @Override
    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    @Override
    public void setSecurityContext(SecurityContext context) {
        this.securityContext = context;
    }

    @Override
    public CallbackHandler createCallbackHandler() {
        return new QuarkusCallbackHandler(null);
    }

    @Override
    public CallbackHandler createCallbackHandler(Callback callback) {
        return new QuarkusCallbackHandler(callback);
    }
}
