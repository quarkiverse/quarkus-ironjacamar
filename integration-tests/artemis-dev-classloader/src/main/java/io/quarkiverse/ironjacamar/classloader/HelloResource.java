package io.quarkiverse.ironjacamar.classloader;

import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/hello")
public class HelloResource {

    @Inject
    ConnectionFactory connectionFactory;

    @GET
    public String greeting() {
        return "Hello, World!";
    }

    @POST
    public void sendMessage() {
        try (JMSContext context = connectionFactory.createContext()) {
            JMSProducer producer = context.createProducer();
            Queue queue = context.createQueue("qa");
            producer.setJMSReplyTo(context.createQueue("reply"));
            producer.send(queue, greeting());
        }
    }
}
