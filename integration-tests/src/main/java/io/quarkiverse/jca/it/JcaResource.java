package io.quarkiverse.jca.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import io.quarkus.narayana.jta.QuarkusTransaction;

@Path("/jca")
@ApplicationScoped
public class JcaResource {
    // add some rest methods here

    @Inject
    ConnectionFactory factory;

    @GET
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public String hello(@QueryParam("name") @DefaultValue("JCA") String name) {
        try (JMSContext context = factory.createContext()) {
            Queue myQueue = context.createQueue("MyQueue");
            JMSProducer producer = context.createProducer();
            for (int i = 0; i < 1; i++) {
                producer.send(myQueue, "Hello " + name);
            }
            System.out.println("MESSAGES SENT");
            if (name.equals("rollback"))
                QuarkusTransaction.setRollbackOnly();
        }
        return "Hello " + name;
    }

    @GET
    @Path("/transacted")
    @Transactional
    public boolean isTransacted() {
        try (JMSContext context = factory.createContext()) {
            return context.getTransacted();
        }
    }

    @GET
    @Path("/not-transacted")
    public boolean isNotTransacted() {
        try (JMSContext context = factory.createContext()) {
            return context.getTransacted();
        }
    }

}
