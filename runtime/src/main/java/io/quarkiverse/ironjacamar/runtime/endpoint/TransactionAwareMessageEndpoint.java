package io.quarkiverse.ironjacamar.runtime.endpoint;

import java.lang.reflect.Method;

import javax.transaction.xa.XAResource;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import com.arjuna.ats.jta.UserTransaction;

import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

/**
 * Transaction aware message endpoint for a given {@link XAResource}
 */
public class TransactionAwareMessageEndpoint implements MessageEndpoint {

    private final Context rootContext;
    private final XAResource xaResource;

    /**
     * Constructor
     *
     * @param rootContext
     * @param xaResource The XA resource
     */
    public TransactionAwareMessageEndpoint(Context rootContext, XAResource xaResource) {
        this.rootContext = rootContext;
        this.xaResource = xaResource;
    }

    /**
     * Initiate a transaction and enlist the XA Resource only if @Transactional is present on the endpoint method
     *
     * @param method The method
     */
    @Override
    public void beforeDelivery(Method method) throws ResourceException {
        ContextInternal ignoredAlwaysNull = ((ContextInternal) rootContext).duplicate().beginDispatch();
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

    /**
     * Commit or rollback the transaction depending on the rollback only status
     */
    @Override
    public void afterDelivery() {
        ContextInternal currentContext = (ContextInternal) Vertx.currentContext();
        try {
            int currentStatus = getStatus();
            if (isActive(currentStatus)) {
                if (currentStatus == Status.STATUS_MARKED_ROLLBACK) {
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
            currentContext.endDispatch(null);
        }
    }

    /**
     * Does nothing
     */
    @Override
    public void release() {
        // Do nothing
    }

    /**
     * Get the current transaction status
     */
    int getStatus() {
        try {
            return UserTransaction.userTransaction().getStatus();
        } catch (SystemException e) {
            throw new QuarkusTransactionException(e);
        }
    }

    /**
     * Check the transaction is active
     */
    boolean isActive(int status) {
        return status == Status.STATUS_ACTIVE ||
                status == Status.STATUS_MARKED_ROLLBACK ||
                status == Status.STATUS_PREPARED ||
                status == Status.STATUS_UNKNOWN ||
                status == Status.STATUS_PREPARING ||
                status == Status.STATUS_COMMITTING ||
                status == Status.STATUS_ROLLING_BACK;
    }
}
