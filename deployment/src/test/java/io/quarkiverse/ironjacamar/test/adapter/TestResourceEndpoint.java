package io.quarkiverse.ironjacamar.test.adapter;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.MessageListener;
import jakarta.resource.cci.Record;

import io.quarkiverse.ironjacamar.ResourceEndpoint;

@ResourceEndpoint(activationSpecConfigKey = "test")
public class TestResourceEndpoint implements MessageListener {

    @Override
    public Record onMessage(Record inputData) throws ResourceException {
        return null;
    }
}
