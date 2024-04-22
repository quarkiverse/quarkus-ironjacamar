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

    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        started = true;
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
}
