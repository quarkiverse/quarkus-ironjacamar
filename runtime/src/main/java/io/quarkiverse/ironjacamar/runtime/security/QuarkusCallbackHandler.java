package io.quarkiverse.ironjacamar.runtime.security;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * Default {@link CallbackHandler} implementation for Quarkus
 */
public class QuarkusCallbackHandler implements CallbackHandler {

    private final org.jboss.jca.core.spi.security.Callback mappings;

    /**
     * Constructor
     *
     * @param mappings The mappings
     */
    public QuarkusCallbackHandler(org.jboss.jca.core.spi.security.Callback mappings) {
        this.mappings = mappings;
    }

    /**
     * Calls the {@link org.jboss.jca.core.spi.security.Callback#mapCallbacks(Callback[])} method.
     */
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        mappings.mapCallbacks(callbacks);
    }
}
