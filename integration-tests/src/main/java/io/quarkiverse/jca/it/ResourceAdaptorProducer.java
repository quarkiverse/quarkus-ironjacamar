package io.quarkiverse.jca.it;

import jakarta.inject.Singleton;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ResourceAdapter;

import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.ra.ActiveMQResourceAdapter;
import org.apache.activemq.artemis.ra.inflow.ActiveMQActivationSpec;

import io.quarkiverse.jca.runtime.spi.ResourceAdapterSupport;

@Singleton
public class ResourceAdaptorProducer implements ResourceAdapterSupport {

    //    @Produces
    //    @Singleton
    //    @Unremovable
    //    public ActiveMQResourceAdapter resourceAdapter() {
    //        ActiveMQResourceAdapter activeMQResourceAdapter = new ActiveMQResourceAdapter();
    //        activeMQResourceAdapter.setConnectorClassName(NettyConnectorFactory.class.getName());
    //        return activeMQResourceAdapter;
    //    }

    //    public void customize(@Observes ActiveMQResourceAdapter activeMQResourceAdapter) {
    //        activeMQResourceAdapter.setConnectorClassName(NettyConnectorFactory.class.getName());
    //    }
    //
    //
    @Override
    public void configureResourceAdapter(ResourceAdapter resourceAdapter) {
        ActiveMQResourceAdapter activeMQResourceAdapter = (ActiveMQResourceAdapter) resourceAdapter;
        activeMQResourceAdapter.setConnectorClassName(NettyConnectorFactory.class.getName());
        activeMQResourceAdapter.setConnectionParameters("host=localhost;port=61616");
        activeMQResourceAdapter.setUseJNDI(false);
        activeMQResourceAdapter.setPassword("quarkus");
        activeMQResourceAdapter.setUserName("quarkus");
    }

    @Override
    public ActivationSpec createActivationSpec(Class<?> type) {
        ActiveMQActivationSpec activationSpec = new ActiveMQActivationSpec();
        // TODO: Read from the config properties map
        //        @ActivationConfigProperty(name = "destinationType", value = "jakarta.jms.Queue"),
        //        @ActivationConfigProperty(name = "maxSession", value = "3"),
        //        @ActivationConfigProperty(name = "destination", value = "MyQueue"),
        //        @ActivationConfigProperty(name = "rebalanceConnections", value = "true")
        activationSpec.setDestinationType("jakarta.jms.Queue");
        activationSpec.setMaxSession(3);
        activationSpec.setDestination("MyQueue");
        activationSpec.setRebalanceConnections(true);
        activationSpec.setUseJNDI(false);
        return activationSpec;
    }
}
