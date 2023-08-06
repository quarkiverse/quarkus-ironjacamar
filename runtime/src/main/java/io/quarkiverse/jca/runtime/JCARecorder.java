package io.quarkiverse.jca.runtime;

import java.util.Set;
import java.util.function.Supplier;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

import io.quarkiverse.jca.runtime.impl.DefaultMessageEndpointFactory;
import io.quarkiverse.jca.runtime.impl.JCAVerticle;
import io.quarkiverse.jca.runtime.spi.ResourceAdapterSupport;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.logging.Log;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;

@Recorder
public class JCARecorder {
    public RuntimeValue<ResourceAdapter> deployResourceAdapter(Supplier<Vertx> vertxSupplier, String resourceAdapterClassName) {
        ResourceAdapter resourceAdapter = null;
        Vertx vertx = vertxSupplier.get();
        try {
            Class<? extends ResourceAdapter> resourceAdapterClass = (Class<? extends ResourceAdapter>) Class
                    .forName(resourceAdapterClassName);
            // TODO: Check if class name matches
            try (InstanceHandle<? extends ResourceAdapter> instanceHandle = Arc.container().instance(resourceAdapterClass)) {
                resourceAdapter = instanceHandle.get();
                // Notify observers
                resourceAdapterSupport().configureResourceAdapter(resourceAdapter);
                Log.tracef("Deploying JCA Resource Adapter: %s ", resourceAdapter);
                vertx.deployVerticle(new JCAVerticle(resourceAdapter));
            }
        } catch (Exception e) {
            //FIXME: bubble up the exception
            Log.error("Cannot deploy", e);
        }
        return new RuntimeValue<>(resourceAdapter);
    }

    public void activateEndpoints(RuntimeValue<ResourceAdapter> resourceAdapterRuntimeValue, Set<String> endpointClassNames) {
        ResourceAdapterSupport resourceAdapterSupport = resourceAdapterSupport();
        ResourceAdapter adapter = resourceAdapterRuntimeValue.getValue();
        for (String endpointClassName : endpointClassNames) {
            Class<?> endpointClass = null;
            try {
                endpointClass = Class.forName(endpointClassName, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            MessageEndpointFactory messageEndpointFactory = new DefaultMessageEndpointFactory(endpointClass,
                    resourceAdapterSupport);
            ActivationSpec activationSpec = resourceAdapterSupport.createActivationSpec(endpointClass);
            try {
                adapter.endpointActivation(messageEndpointFactory, activationSpec);
            } catch (ResourceException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static ResourceAdapterSupport resourceAdapterSupport() {
        return Arc.container().instance(ResourceAdapterSupport.class).get();
    }
}
