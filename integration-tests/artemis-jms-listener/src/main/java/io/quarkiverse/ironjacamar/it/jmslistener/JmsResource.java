package io.quarkiverse.ironjacamar.it.jmslistener;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/jms")
@Produces(MediaType.APPLICATION_JSON)
public class JmsResource {

    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    JmsListenerApp listenerApp;

    @POST
    @Path("/send")
    public void send(String message) {
        try (JMSContext ctx = connectionFactory.createContext()) {
            ctx.createProducer().send(ctx.createQueue("test-queue"), message);
        }
    }

    @GET
    @Path("/received")
    public List<String> received() {
        return listenerApp.getReceived();
    }
}
