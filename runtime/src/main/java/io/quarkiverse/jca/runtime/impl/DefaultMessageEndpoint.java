package io.quarkiverse.jca.runtime.impl;

import java.lang.reflect.Method;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.endpoint.MessageEndpoint;

public class DefaultMessageEndpoint implements MessageEndpoint {
    @Override
    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {

    }

    @Override
    public void afterDelivery() throws ResourceException {

    }

    @Override
    public void release() {

    }
}
