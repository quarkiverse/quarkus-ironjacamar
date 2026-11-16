package io.quarkiverse.ironjacamar.test.adapter;

import javax.transaction.xa.XAResource;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

/**
 * A {@link ResourceAdapter} that fails to start, or fails to activate its endpoints.
 */
public class FailingResourceAdapter implements ResourceAdapter {

    /** The message of the exception thrown by {@link #start(BootstrapContext)} */
    public static final String START_FAILURE_MESSAGE = "the resource adapter cannot start";

    /** The message of the exception thrown by {@link #endpointActivation(MessageEndpointFactory, ActivationSpec)} */
    public static final String ACTIVATION_FAILURE_MESSAGE = "the endpoint cannot be activated";

    private final boolean failOnStart;

    public FailingResourceAdapter(boolean failOnStart) {
        this.failOnStart = failOnStart;
    }

    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        if (failOnStart) {
            throw new ResourceAdapterInternalException(START_FAILURE_MESSAGE);
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec)
            throws ResourceException {
        throw new ResourceException(ACTIVATION_FAILURE_MESSAGE);
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) {
        return new XAResource[0];
    }
}
