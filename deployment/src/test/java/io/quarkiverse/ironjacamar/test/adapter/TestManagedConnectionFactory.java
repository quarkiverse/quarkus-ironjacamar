package io.quarkiverse.ironjacamar.test.adapter;

import java.io.PrintWriter;
import java.util.Set;

import javax.security.auth.Subject;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

public class TestManagedConnectionFactory implements ManagedConnectionFactory {
    @Override
    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        return new TestConnectionFactory();
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return new TestConnectionFactory();
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        return null;
    }

    @Override
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {

    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return null;
    }
}
