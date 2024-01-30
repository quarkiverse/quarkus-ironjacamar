package io.quarkiverse.ironjacamar.runtime.security;

import javax.security.auth.Subject;

import org.jboss.jca.core.spi.security.SecurityContext;

/**
 * Default {@link SecurityContext} implementation for Quarkus
 */
public class QuarkusSecurityContext implements SecurityContext {
    private Subject subject;

    /**
     * Constructor
     */
    public QuarkusSecurityContext() {

    }

    /**
     * @return the authenticated subject
     */
    @Override
    public Subject getAuthenticatedSubject() {
        return subject;
    }

    /**
     * Set the authenticated subject
     *
     * @param subject The Subject
     */
    @Override
    public void setAuthenticatedSubject(Subject subject) {
        this.subject = subject;
    }

    /**
     * @return the roles
     */
    @Override
    public String[] getRoles() {
        return new String[0];
    }
}
