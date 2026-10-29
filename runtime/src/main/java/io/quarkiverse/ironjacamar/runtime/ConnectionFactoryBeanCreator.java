package io.quarkiverse.ironjacamar.runtime;

import javax.naming.Reference;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.smallrye.common.annotation.Identifier;

/**
 * Creates a connection factory from the given resource adapter (implementation vendor specific).
 */
public class ConnectionFactoryBeanCreator implements BeanCreator<Object> {

    @Override
    public Object create(SyntheticCreationalContext<Object> context) {
        String id = (String) context.getParams().get("id");
        IronJacamarContainer container = context.getInjectedReference(IronJacamarContainer.class,
                Identifier.Literal.of(id));
        try {
            Object connectionFactory = container.createConnectionFactory();
            if (connectionFactory instanceof Referenceable ref) {
                // Set a reference to avoid serialization and JNDI lookups. See https://issues.apache.org/jira/browse/ARTEMIS-5253
                ref.setReference(new Reference(connectionFactory.getClass().getCanonicalName(), null, null));
            }
            return connectionFactory;
        } catch (ResourceException e) {
            throw new DeploymentException("Cannot create connection factory", e);
        }
    }
}
