package io.quarkiverse.jca.it;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/jca")
@ApplicationScoped
public class JcaResource {
    // add some rest methods here

    @Inject
    ConnectionFactory factory;

    @Inject
    EntityManager entityManager;

    @GET
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public String hello(@QueryParam("name") @DefaultValue("JCA") String name) {
        MyEntity o = new MyEntity();
        o.id = 1L;
        o.name = name;
        entityManager.persist(o);
        try (JMSContext context = factory.createContext()) {
            Queue myQueue = context.createQueue("MyQueue");
            JMSProducer producer = context.createProducer();
            for (int i = 0; i < 2; i++) {
                producer.send(myQueue, "Hello " + name);
            }
            System.out.println("MESSAGES SENT");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (name.equals("rollback"))
                QuarkusTransaction.setRollbackOnly();
        }
        System.out.println(entityManager.find(MyEntity.class, 1L));
//        QuarkusTransaction.setRollbackOnly();
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
