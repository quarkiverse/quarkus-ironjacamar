package io.quarkiverse.jca.it;

import java.lang.reflect.Method;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.endpoint.MessageEndpoint;

import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.ra.ActiveMQResourceAdapter;
import org.apache.activemq.artemis.ra.inflow.ActiveMQActivationSpec;

import io.quarkiverse.jca.runtime.spi.ResourceAdapterSupport;

@Singleton
public class AppResourceAdaptorSupport implements ResourceAdapterSupport {

    /**
     * Required to @Inject ConnectionFactory in classes
     */
    @Produces
    @ApplicationScoped
    public ActiveMQConnectionFactory createConnectionFactory(ActiveMQResourceAdapter adapter) {
        return adapter.getConnectionFactory(adapter.getProperties());
    }

    @Override
    public void configureResourceAdapter(ResourceAdapter resourceAdapter) {
        ActiveMQResourceAdapter activeMQResourceAdapter = (ActiveMQResourceAdapter) resourceAdapter;
        activeMQResourceAdapter.setConnectorClassName(NettyConnectorFactory.class.getName());
        activeMQResourceAdapter
                .setConnectionParameters("host=localhost;port=5445;protocols=HORNETQ");
        activeMQResourceAdapter.setProtocolManagerFactoryStr(
                "org.apache.activemq.artemis.core.protocol.hornetq.client.HornetQClientProtocolManagerFactory");
        activeMQResourceAdapter.setUseJNDI(false);
        activeMQResourceAdapter.setIgnoreJTA(false);
        activeMQResourceAdapter.setPassword("quarkus");
        activeMQResourceAdapter.setUserName("quarkus");
    }

    @Override
    public ActivationSpec createActivationSpec(ResourceAdapter resourceAdapter, Class<?> type) throws Exception {
        ActiveMQResourceAdapter activeMQResourceAdapter = (ActiveMQResourceAdapter) resourceAdapter;
        ActiveMQActivationSpec activationSpec = new ActiveMQActivationSpec();
        // TODO: Read from the config properties map
        activationSpec.setDestinationType("jakarta.jms.Queue");
        activationSpec.setMaxSession(10);
        activationSpec.setDestination("jms.queue.MyQueue");
        activationSpec.setRebalanceConnections(true);
        activationSpec.setUseJNDI(false);
        activationSpec.setUserName(activeMQResourceAdapter.getUserName());
        activationSpec.setPassword(activeMQResourceAdapter.getPassword());
        return activationSpec;
    }

    @Override
    public MessageEndpoint wrap(Object resourceEndpoint, MessageEndpoint messageEndpoint) {
        return new MessageEndpointWrapper((MessageListener) resourceEndpoint, messageEndpoint);
    }

    private static class MessageEndpointWrapper implements MessageEndpoint, MessageListener {

        private final MessageEndpoint delegate;
        private final MessageListener listener;

        private MessageEndpointWrapper(MessageListener listener, MessageEndpoint delegate) {
            this.listener = listener;
            this.delegate = delegate;
        }

        @Override
        public void onMessage(Message message) {
            listener.onMessage(message);
        }

        @Override
        public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {
            delegate.beforeDelivery(method);
        }

        @Override
        public void afterDelivery() throws ResourceException {
            delegate.afterDelivery();
        }

        @Override
        public void release() {
            delegate.release();
        }
    }
}
