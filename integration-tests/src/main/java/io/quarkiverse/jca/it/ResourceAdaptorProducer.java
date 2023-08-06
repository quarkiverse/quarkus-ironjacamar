package io.quarkiverse.jca.it;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.resource.spi.ResourceAdapter;

import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.ra.ActiveMQResourceAdapter;

import io.quarkus.arc.Unremovable;

public class ResourceAdaptorProducer {

//    @Produces
//    @Singleton
//    @Unremovable
//    public ResourceAdapter resourceAdapter() {
//        ActiveMQResourceAdapter activeMQResourceAdapter = new ActiveMQResourceAdapter();
//        activeMQResourceAdapter.setConnectorClassName(NettyConnectorFactory.class.getName());
//        return activeMQResourceAdapter;
//    }

    public void customize(@Observes ActiveMQResourceAdapter activeMQResourceAdapter) {
        System.out.println("OBSERVER CALLED! ");
        activeMQResourceAdapter.setConnectorClassName(NettyConnectorFactory.class.getName());
    }
}
