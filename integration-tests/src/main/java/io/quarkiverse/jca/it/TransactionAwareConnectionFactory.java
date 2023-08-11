package io.quarkiverse.jca.it;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.transaction.TransactionManager;

import org.apache.activemq.artemis.ra.ActiveMQRAConnectionFactory;
import org.jboss.narayana.jta.jms.ConnectionFactoryProxy;
import org.jboss.narayana.jta.jms.TransactionHelperImpl;

import io.quarkus.narayana.jta.QuarkusTransaction;

public class TransactionAwareConnectionFactory implements ConnectionFactory {

    private final ConnectionFactory delegate;
    private final ConnectionFactory transactionAwareDelegate;

    public TransactionAwareConnectionFactory(ActiveMQRAConnectionFactory delegate, TransactionManager transactionManager) {
        this.delegate = delegate;
        TransactionHelperImpl transactionHelper = new TransactionHelperImpl(transactionManager);
        this.transactionAwareDelegate = new ConnectionFactoryProxy(delegate, transactionHelper);
    }

    @Override
    public Connection createConnection() throws JMSException {
        if (QuarkusTransaction.isActive()) {
            return transactionAwareDelegate.createConnection();
        } else {
            return delegate.createConnection();
        }
    }

    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        if (QuarkusTransaction.isActive()) {
            return transactionAwareDelegate.createConnection(userName, password);
        } else {
            return delegate.createConnection(userName, password);
        }
    }

    @Override
    public JMSContext createContext() {
        if (QuarkusTransaction.isActive()) {
            return transactionAwareDelegate.createContext();
        } else {
            return delegate.createContext();
        }
    }

    @Override
    public JMSContext createContext(String userName, String password) {
        if (QuarkusTransaction.isActive()) {
            return transactionAwareDelegate.createContext(userName, password);
        } else {
            return delegate.createContext(userName, password);
        }
    }

    @Override
    public JMSContext createContext(String userName, String password, int sessionMode) {
        if (QuarkusTransaction.isActive()) {
            return transactionAwareDelegate.createContext(userName, password, sessionMode);
        } else {
            return delegate.createContext(userName, password, sessionMode);
        }
    }

    @Override
    public JMSContext createContext(int sessionMode) {
        if (QuarkusTransaction.isActive()) {
            return transactionAwareDelegate.createContext(sessionMode);
        } else {
            return delegate.createContext(sessionMode);
        }
    }
}
