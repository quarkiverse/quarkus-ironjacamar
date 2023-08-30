package io.quarkiverse.ironjacamar;

import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.transaction.Transactional;

import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;

@ResourceEndpoint(activationSpecConfigKey = "myqueue")
public class MyMessageEndpoint implements MessageListener {

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onMessage(Message message) {
        try {
            Log.infof("Redelivered: %s",message.getJMSRedelivered());
            Log.infof("Transaction is Active? %s", QuarkusTransaction.isActive());
            String body = message.getBody(String.class);
            Log.infof("Received message: %s", body);
            if (body.contains("George")) {
                QuarkusTransaction.setRollbackOnly();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
