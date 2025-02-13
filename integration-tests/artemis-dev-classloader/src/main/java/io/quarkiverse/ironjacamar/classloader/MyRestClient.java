package io.quarkiverse.ironjacamar.classloader;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/hello")
@RegisterRestClient(configKey = "foo")
public interface MyRestClient {

    @GET
    String hello();
}
