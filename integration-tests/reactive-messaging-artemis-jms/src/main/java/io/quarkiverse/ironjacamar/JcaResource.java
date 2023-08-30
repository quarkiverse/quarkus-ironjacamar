package io.quarkiverse.ironjacamar;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.quarkus.narayana.jta.QuarkusTransaction;

@Path("/jca")
@ApplicationScoped
public class JcaResource {
    // add some rest methods here

    @Inject
    ConnectionFactory factory;

    @POST
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void sendToSales(@FormParam("name") @DefaultValue("JCA") String name) {
        try (JMSContext context = factory.createContext()) {
            Destination salesQueue = context.createQueue("sales");
            JMSProducer producer = context.createProducer();
            producer.send(salesQueue, "Hello " + name);
            if (name.equals("rollback"))
                QuarkusTransaction.setRollbackOnly();
        }
    }

    @Incoming("sales")
    @Outgoing("inventory")
    public String hello(Message<String> message) {
        return "Hello Reactive Messaging : " + message.getPayload();
    }

}
