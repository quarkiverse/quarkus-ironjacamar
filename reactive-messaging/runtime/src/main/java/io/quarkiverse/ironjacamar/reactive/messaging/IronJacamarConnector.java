package io.quarkiverse.ironjacamar.reactive.messaging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;

import io.quarkiverse.ironjacamar.Defaults;
import io.quarkiverse.ironjacamar.ResourceAdapterKind;
import io.quarkiverse.ironjacamar.runtime.IronJacamarContainer;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.reactive.messaging.connector.InboundConnector;

/**
 * SmallRye Reactive Messaging connector backed by IronJacamar JCA resource adapters.
 * <p>
 * This connector delegates to {@link ReactiveMessagingResourceAdapterSupport} SPI implementations
 * (qualified with {@link ResourceAdapterKind}) to bridge transport-specific listener interfaces
 * into Reactive Messaging channels.
 */
@ApplicationScoped
@Connector(IronJacamarConnector.CONNECTOR_NAME)
public class IronJacamarConnector implements InboundConnector {

    static final String CONNECTOR_NAME = "ironjacamar";

    static final String RESOURCE_ADAPTER_KIND = "resource-adapter-kind";
    static final String RESOURCE_ADAPTER_NAME = "resource-adapter-name";
    static final String ACTIVATION_SPEC_CONFIG_KEY = "activation-spec-config-key";

    private static final Set<String> RESERVED_KEYS = Set.of(
            ConnectorFactory.CHANNEL_NAME_ATTRIBUTE,
            ConnectorFactory.CONNECTOR_ATTRIBUTE,
            RESOURCE_ADAPTER_KIND,
            RESOURCE_ADAPTER_NAME,
            ACTIVATION_SPEC_CONFIG_KEY);

    @Inject
    @Any
    Instance<IronJacamarContainer> containers;

    @Inject
    @Any
    Instance<ReactiveMessagingResourceAdapterSupport> supportInstances;

    private final List<IronJacamarIncomingChannel> channels = new CopyOnWriteArrayList<>();

    @Override
    public Flow.Publisher<? extends Message<?>> getPublisher(Config config) {
        String channelName = config.getValue(ConnectorFactory.CHANNEL_NAME_ATTRIBUTE, String.class);

        String kind = config.getValue(RESOURCE_ADAPTER_KIND, String.class);
        String raName = config.getOptionalValue(RESOURCE_ADAPTER_NAME, String.class)
                .orElse(Defaults.DEFAULT_RESOURCE_ADAPTER_NAME);

        ReactiveMessagingResourceAdapterSupport support = supportInstances
                .select(ResourceAdapterKind.Literal.of(kind))
                .get();

        IronJacamarContainer container = containers
                .select(Identifier.Literal.of(raName))
                .get();

        Map<String, String> channelConfig = extractChannelConfig(config);
        Map<String, String> activationSpecConfig = support.mapToActivationSpecConfig(channelConfig);

        IronJacamarIncomingChannel channel = new IronJacamarIncomingChannel(
                channelName, kind, raName, container, support, activationSpecConfig);
        channels.add(channel);

        return channel.getPublisher();
    }

    @PreDestroy
    void close() {
        for (IronJacamarIncomingChannel channel : channels) {
            channel.close();
        }
        channels.clear();
    }

    private static Map<String, String> extractChannelConfig(Config config) {
        Map<String, String> result = new HashMap<>();
        for (String key : config.getPropertyNames()) {
            if (!RESERVED_KEYS.contains(key)) {
                config.getOptionalValue(key, String.class)
                        .ifPresent(value -> result.put(key, value));
            }
        }
        return result;
    }
}
