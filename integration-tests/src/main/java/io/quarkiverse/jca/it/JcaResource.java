package io.quarkiverse.jca.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;

@Path("/jca")
@ApplicationScoped
public class JcaResource {
    // add some rest methods here

    @Inject
    ConnectionFactory factory;

    @GET
    @Transactional
    public Uni<String> hello(@QueryParam("name") @DefaultValue("JCA") String name) {
        return Uni.createFrom().item(Unchecked.supplier(() -> {
            try (JMSContext context = factory.createContext()) {
                Queue myQueue = context.createQueue("MyQueue");
                for (int i = 0; i < 10; i++) {
                    context.createProducer().send(myQueue, "Hello " + name);
                }
                return "Hello " + name;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    @GET
    @Path("/transacted")
    @Transactional
    public boolean isTransacted() {
        try (JMSContext context = factory.createContext()) {
            return context.getTransacted();
        }
    }

}
