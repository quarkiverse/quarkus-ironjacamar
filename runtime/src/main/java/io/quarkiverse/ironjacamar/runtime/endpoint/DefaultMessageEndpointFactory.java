package io.quarkiverse.ironjacamar.runtime.endpoint;

import java.lang.reflect.Method;
import java.util.Objects;

import javax.transaction.xa.XAResource;

import jakarta.resource.spi.UnavailableException;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.transaction.Transactional;

import io.quarkiverse.ironjacamar.Defaults;
import io.quarkiverse.ironjacamar.ResourceAdapterFactory;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;

/**
 * Default implementation of {@link MessageEndpointFactory}.
 */
public class DefaultMessageEndpointFactory implements MessageEndpointFactory {

    private final Vertx vertx;
    private final Class<?> endpointClass;
    private final String identifier;
    private final ResourceAdapterFactory resourceAdapterSupport;
    private final ClassLoader classLoader;
    private Object endpointInstance;
    private Boolean transacted;

    /**
     * Constructor
     *
     * @param endpointClass The endpoint class
     * @param identifier The identifier
     * @param adapterFactory The resource adapter factory
     */
    public DefaultMessageEndpointFactory(Vertx vertx, Class<?> endpointClass, String identifier,
            ResourceAdapterFactory adapterFactory) {
        this.vertx = vertx;
        this.endpointClass = endpointClass;
        this.identifier = identifier;
        this.resourceAdapterSupport = adapterFactory;
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    /**
     * Constructor
     *
     * @param endpointClass The endpoint class
     * @param identifier The identifier
     * @param adapterFactory The resource adapter factory
     */
    public DefaultMessageEndpointFactory(Vertx vertx, Object endpointInstance,
            ResourceAdapterFactory adapterFactory) {
        this.vertx = vertx;
        this.endpointClass = endpointInstance.getClass();
        // Unused
        this.identifier = null;
        this.resourceAdapterSupport = adapterFactory;
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public boolean isDeliveryTransacted(Method method) throws NoSuchMethodException {
        if (transacted == null) {
            Method endpointClassMethod = endpointClass.getMethod(method.getName(), method.getParameterTypes());
            Transactional annotation = endpointClassMethod.getAnnotation(Transactional.class);
            if (annotation == null) {
                transacted = Boolean.FALSE;
            } else {
                transacted = annotation.value() != Transactional.TxType.NEVER &&
                        annotation.value() != Transactional.TxType.NOT_SUPPORTED;
            }
        }
        return transacted;
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException {
        // TODO: Implement timeout
        return createEndpoint(xaResource);
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource) {
        MessageEndpoint endpoint;
        if (xaResource == null) {
            endpoint = NoopMessageEndpoint.INSTANCE;
        } else {
            endpoint = new TransactionAwareMessageEndpoint(xaResource);
        }
        // When running in dev mode, we don't want to wrap the endpoint
        if (LaunchMode.current() != LaunchMode.NORMAL) {
            endpoint = new ClassLoaderMessageEndpoint(endpoint, classLoader);
        }
        // Duplicate context
        endpoint = new DuplicatedContextMessageEndpoint(endpoint, vertx.getOrCreateContext());
        return resourceAdapterSupport.wrap(endpoint, getEndpointInstance());
    }

    @Override
    public String getActivationName() {
        return endpointClass.getName();
    }

    @Override
    public Class<?> getEndpointClass() {
        return endpointClass;
    }

    private Object getEndpointInstance() {
        if (endpointInstance == null) {
            ArcContainer container = Arc.container();
            if (Defaults.DEFAULT_RESOURCE_ADAPTER_NAME.equals(identifier)) {
                // Try with default identifier and fallback to default if null
                endpointInstance = container.select(endpointClass, Identifier.Literal.of(identifier)).orElse(null);
                if (endpointInstance == null) {
                    endpointInstance = container.select(endpointClass).get();
                }
            } else {
                endpointInstance = container.select(endpointClass, Identifier.Literal.of(identifier)).get();
            }
        }
        return Objects.requireNonNull(endpointInstance,
                () -> "Unable to find endpoint instance for %s with identifier %s in Arc container"
                        .formatted(endpointClass.getName(), identifier));
    }

}
