package io.quarkiverse.ironjacamar.test.adapter;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.MessageListener;
import jakarta.resource.cci.Record;

/**
 * An endpoint that is <em>not</em> annotated with {@link io.quarkiverse.ironjacamar.ResourceEndpoint}, standing in for
 * the endpoints another extension contributes through a
 * {@code io.quarkiverse.ironjacamar.deployment.ResourceEndpointBuildItem}.
 */
public class ContributedResourceEndpoint implements MessageListener {

    @Override
    public Record onMessage(Record inputData) throws ResourceException {
        return null;
    }
}
