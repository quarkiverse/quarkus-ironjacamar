package io.quarkiverse.ironjacamar.runtime.security;

import javax.security.auth.Subject;

import org.jboss.jca.core.spi.security.SecurityContext;

public class QuarkusSecurityContext implements SecurityContext {
    private Subject subject;

    @Override
    public Subject getAuthenticatedSubject() {
        return subject;
    }

    @Override
    public void setAuthenticatedSubject(Subject subject) {
        this.subject = subject;
    }

    @Override
    public String[] getRoles() {
        return new String[0];
    }
}
