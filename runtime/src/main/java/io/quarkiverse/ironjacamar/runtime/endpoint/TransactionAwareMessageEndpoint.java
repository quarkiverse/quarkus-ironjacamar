package io.quarkiverse.ironjacamar.runtime.endpoint;

import java.lang.reflect.Method;

import javax.transaction.xa.XAResource;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;

public class TransactionAwareMessageEndpoint implements MessageEndpoint {

    private final XAResource xaResource;

    public TransactionAwareMessageEndpoint(XAResource xaResource) {
        this.xaResource = xaResource;
    }

    /**
     * Initiate a transaction and enlist the XA Resource only if @Transactional is present on the endpoint method
     */
    @Override
    public void beforeDelivery(Method method) throws ResourceException {
        Arc.container().requestContext().activate();
        QuarkusTransaction.begin();
        try {
            Transaction transaction = Arc.container().select(TransactionManager.class).get().getTransaction();
            // Enlisting the resource so the message delivery is part of the transaction
            // See https://jakarta.ee/specifications/connectors/2.1/jakarta-connectors-spec-2.1#transacted-delivery-using-container-managed-transaction
            if (!transaction.enlistResource(xaResource)) {
                throw new ResourceException("Cannot enlist resource");
            }
        } catch (RollbackException | SystemException e) {
            throw new ResourceException("Error while enlisting resource", e);
        }
    }

    @Override
    public void afterDelivery() {
        try {
            if (QuarkusTransaction.isActive()) {
                if (QuarkusTransaction.isRollbackOnly()) {
                    QuarkusTransaction.rollback();
                } else {
                    QuarkusTransaction.commit();
                }
            }
        } finally {
            var context = Arc.container().requestContext();
            if (context.isActive()) {
                context.deactivate();
            }
        }
    }

    @Override
    public void release() {

    }
}
