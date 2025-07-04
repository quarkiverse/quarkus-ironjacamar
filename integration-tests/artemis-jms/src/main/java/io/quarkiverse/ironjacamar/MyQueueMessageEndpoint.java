package io.quarkiverse.ironjacamar;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Singleton;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;

import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Vertx;

@Singleton
@ResourceEndpoint(activationSpecConfigKey = "myqueue")
@Path("/myqueue")
public class MyQueueMessageEndpoint implements MessageListener {

    AtomicInteger counter = new AtomicInteger(0);

    @Override
    @Transactional
    public void onMessage(Message message) {
        try {
            Log.info("####### Vertx.context = " + Vertx.currentContext());
            assert VertxContext.isOnDuplicatedContext();
            String body = message.getBody(String.class);
            Log.infof("Received message: %s", body);
            counter.incrementAndGet();
            if (body.contains("rollback")) {
                QuarkusTransaction.setRollbackOnly();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PUT
    @Path("/reset")
    public void resetCounter() {
        counter.set(0);
    }

    @GET
    public int getCounter() {
        return counter.get();
    }
}
