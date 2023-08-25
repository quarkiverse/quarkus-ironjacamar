package io.quarkiverse.ironjacamar;

import java.lang.reflect.Method;
import java.util.Map;

import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.endpoint.MessageEndpoint;

import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.ra.ActiveMQRAManagedConnectionFactory;
import org.apache.activemq.artemis.ra.ActiveMQResourceAdapter;
import org.apache.activemq.artemis.ra.inflow.ActiveMQActivationSpec;

/**
 * This would be in the artemis-jms extension
 */
@ResourceAdapterKind(ArtemisResourceAdapterFactory.KIND)
public class ArtemisResourceAdapterFactory implements ResourceAdapterFactory {

    static final String KIND = "artemis";

    @Override
    public ActiveMQResourceAdapter createResourceAdapter(Map<String, String> config) {
        ActiveMQResourceAdapter adapter = new ActiveMQResourceAdapter();
        adapter.setConnectorClassName(NettyConnectorFactory.class.getName());
        adapter.setConnectionParameters(config.get("connection-parameters"));
        adapter.setProtocolManagerFactoryStr(config.get("protocol-manager-factory"));
        adapter.setUseJNDI(false);
        adapter.setUserName(config.get("user"));
        adapter.setPassword(config.get("password"));
        return adapter;
    }

    @Override
    public ManagedConnectionFactory createManagedConnectionFactory(ResourceAdapter adapter)
            throws ResourceException {
        ActiveMQRAManagedConnectionFactory factory = new ActiveMQRAManagedConnectionFactory();
        factory.setResourceAdapter(adapter);
        return factory;
    }

    @Override
    public ActivationSpec createActivationSpec(ResourceAdapter adapter, Class<?> type, Map<String, String> config)
            throws ResourceException {
        //TODO: Use the config
        ActiveMQActivationSpec activationSpec = new ActiveMQActivationSpec();
        activationSpec.setResourceAdapter(adapter);
        activationSpec.setDestinationType("jakarta.jms.Queue");
        activationSpec.setMaxSession(2);
        activationSpec.setDestination("jms.queue.MyQueue");
        activationSpec.setRebalanceConnections(true);
        activationSpec.setUseJNDI(false);
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
