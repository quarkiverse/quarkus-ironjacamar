package io.quarkiverse.ironjacamar;

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

import io.quarkiverse.ironjacamar.runtime.endpoint.MessageEndpointWrapper;

/**
 * This would be in the artemis-jms extension
 */
@ResourceAdapterKind(value = "artemis")
@ResourceAdapterTypes(connectionFactoryTypes = { jakarta.jms.ConnectionFactory.class })
public class ArtemisResourceAdapterFactory implements ResourceAdapterFactory {

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
        ActiveMQActivationSpec activationSpec = new ActiveMQActivationSpec();
        activationSpec.setResourceAdapter(adapter);
        activationSpec.setDestinationType(config.get("destination-type"));
        activationSpec.setDestination(config.get("destination"));
        activationSpec.setMaxSession(Integer.valueOf(config.getOrDefault("max-session", "5")));
        activationSpec.setRebalanceConnections(Boolean.valueOf(config.getOrDefault("rebalance-connections", "true")));
        activationSpec.setUseJNDI(false);
        activationSpec.setUseLocalTx(false);
        return activationSpec;
    }

    @Override
    public MessageEndpoint wrap(MessageEndpoint endpoint, Object resourceEndpoint) {
        return new JMSMessageEndpoint(endpoint, (MessageListener) resourceEndpoint);
    }

    private static class JMSMessageEndpoint extends MessageEndpointWrapper implements MessageListener {

        private final MessageListener listener;

        private JMSMessageEndpoint(MessageEndpoint messageEndpoint, MessageListener listener) {
            super(messageEndpoint);
            this.listener = listener;
        }

        @Override
        public void onMessage(Message message) {
            listener.onMessage(message);
        }
    }
}
