package io.quarkiverse.ironjacamar;

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

import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.smallrye.common.annotation.Identifier;

@Path("/jca")
@ApplicationScoped
public class JcaResource {
    // add some rest methods here

    @Inject
    @Identifier("other")
    ConnectionFactory otherFactory;

    @GET
    @Transactional
    public void hello(@QueryParam("name") @DefaultValue("JCA") String name) {
        try (JMSContext context = otherFactory.createContext()) {
            Queue myQueue = context.createQueue("MyQueue");
            JMSProducer producer = context.createProducer();
            producer.send(myQueue, "Hello " + name);
            Log.info("MESSAGES SENT");
            if (name.equals("rollback"))
                QuarkusTransaction.setRollbackOnly();
        }
    }
}
