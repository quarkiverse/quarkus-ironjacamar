package io.quarkiverse.jca.it;

import jakarta.enterprise.event.Observes;

import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.ra.ActiveMQResourceAdapter;

public class ResourceAdaptorProducer {

    //    @Produces
    //    @Singleton
    //    @Unremovable
    //    public ActiveMQResourceAdapter resourceAdapter() {
    //        ActiveMQResourceAdapter activeMQResourceAdapter = new ActiveMQResourceAdapter();
    //        activeMQResourceAdapter.setConnectorClassName(NettyConnectorFactory.class.getName());
    //        return activeMQResourceAdapter;
    //    }

    public void customize(@Observes ActiveMQResourceAdapter activeMQResourceAdapter) {
        activeMQResourceAdapter.setConnectorClassName(NettyConnectorFactory.class.getName());
    }
}
