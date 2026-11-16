package io.quarkiverse.ironjacamar.test.adapter;

import javax.transaction.xa.XAResource;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

public class TestResourceAdapter implements ResourceAdapter {

    private boolean started;

    private volatile BootstrapContext bootstrapContext;

    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        started = true;
        bootstrapContext = ctx;
    }

    @Override
    public void stop() {
        started = false;
    }

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {

    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {

    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        return new XAResource[0];
    }

    public boolean isStarted() {
        return started;
    }

    /**
     * Get the bootstrap context this resource adapter was started with
     *
     * @return the bootstrap context, or {@code null} if the resource adapter was never started
     */
    public BootstrapContext getBootstrapContext() {
        return bootstrapContext;
    }
}
