package io.quarkiverse.ironjacamar.reactive.it;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/messages")
public class MessageResource {

    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    MessageConsumer consumer;

    @Inject
    MessageProducer producer;

    @POST
    public void send(@QueryParam("text") @DefaultValue("hello") String text) {
        try (JMSContext context = connectionFactory.createContext()) {
            Queue queue = context.createQueue("TestQueue");
            JMSProducer jmsProducer = context.createProducer();
            jmsProducer.send(queue, text);
        }
    }

    @POST
    @Path("/outgoing")
    public void sendViaOutgoing(@QueryParam("text") @DefaultValue("hello") String text) {
        producer.send(text);
    }

    @GET
    @Path("/outgoing")
    public String receiveFromOutQueue() {
        try (JMSContext context = connectionFactory.createContext()) {
            Queue queue = context.createQueue("OutQueue");
            JMSConsumer jmsConsumer = context.createConsumer(queue);
            return jmsConsumer.receiveBody(String.class, 5000);
        }
    }

    @GET
    public List<String> received() {
        return consumer.getReceived();
    }

    @GET
    @Path("/count")
    public int count() {
        return consumer.getReceived().size();
    }

    @DELETE
    public void clear() {
        consumer.clear();
    }
}
