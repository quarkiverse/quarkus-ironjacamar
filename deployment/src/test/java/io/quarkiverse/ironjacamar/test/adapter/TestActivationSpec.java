package io.quarkiverse.ironjacamar.test.adapter;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;

public class TestActivationSpec implements ActivationSpec {

    private final ResourceAdapter adapter;

    public TestActivationSpec(ResourceAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void validate() throws InvalidPropertyException {

    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {

    }
}
