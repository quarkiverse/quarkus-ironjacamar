package io.quarkiverse.ironjacamar.test.adapter;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.MessageListener;
import jakarta.resource.cci.Record;

import io.quarkiverse.ironjacamar.ResourceEndpoint;

/**
 * A second annotated endpoint, used to assert that only the expected endpoints are ignored.
 */
@ResourceEndpoint(activationSpecConfigKey = "other")
public class OtherTestResourceEndpoint implements MessageListener {

    @Override
    public Record onMessage(Record inputData) throws ResourceException {
        return null;
    }
}
