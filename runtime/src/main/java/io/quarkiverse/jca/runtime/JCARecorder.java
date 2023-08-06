package io.quarkiverse.jca.runtime;

import io.quarkiverse.jca.runtime.vertx.JCAVerticle;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.logging.Log;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.resource.spi.ResourceAdapter;

import java.util.Map;
import java.util.function.Supplier;

@Recorder
public class JCARecorder {
    public ResourceAdapter deployResourceAdapter(Supplier<Vertx> vertxSupplier, String resourceAdapterClassName,
                                                 Map<String, String> configProperties) {
        ResourceAdapter resourceAdapter = null;
        Vertx vertx = vertxSupplier.get();
        try {
            Class<? extends ResourceAdapter> resourceAdapterClass = (Class<? extends ResourceAdapter>) Class.forName(resourceAdapterClassName);
            // TODO: Check if class name matches
            try (InstanceHandle<? extends ResourceAdapter> instanceHandle = Arc.container().instance(resourceAdapterClass)) {
                resourceAdapter = instanceHandle.get();
                BeanManager beanManager = Arc.container().beanManager();
                // Notify observers
                beanManager.getEvent().fire(resourceAdapter);
                Log.tracef("Deploying JCA Resource Adapter: %s ", resourceAdapter);
                vertx.deployVerticle(new JCAVerticle(resourceAdapter));
            }
        } catch (Exception e) {
            //FIXME: bubble up the exception
            Log.error("Cannot deploy", e);
        }
        return resourceAdapter;
    }
}
