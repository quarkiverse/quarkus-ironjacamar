package io.quarkiverse.ironjacamar;

import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.transaction.Transactional;

import io.quarkus.logging.Log;

@ResourceEndpoint(activationSpecConfigKey = "myqueue")
public class MyQueueMessageEndpoint implements MessageListener {

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onMessage(Message message) {
        try {
            String body = message.getBody(String.class);
            Log.infof("Received message: %s", body);
            if (body.contains("George")) {
                //                QuarkusTransaction.setRollbackOnly();
                System.out.println("ROLLBACK");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
