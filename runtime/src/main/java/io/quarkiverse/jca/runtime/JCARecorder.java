package io.quarkiverse.jca.runtime;

import java.util.Map;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.resource.spi.ResourceAdapter;

import io.quarkiverse.jca.runtime.vertx.JCAVerticle;
import io.quarkus.arc.Arc;
import io.quarkus.logging.Log;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;

@Recorder
public class JCARecorder {
    public ResourceAdapter deployResourceAdapter(Supplier<Vertx> vertxSupplier, String resourceAdapterClassName,
            Map<String, String> configProperties) {
        ResourceAdapter resourceAdapter = null;
        Vertx vertx = vertxSupplier.get();
        try {
            // TODO: Check if class name matches
            resourceAdapter = Arc.container().instance(ResourceAdapter.class).get();
            if (resourceAdapter == null) {
                // Could not find a producer, create it manually
                resourceAdapter = (ResourceAdapter) Class.forName(resourceAdapterClassName).getDeclaredConstructor()
                        .newInstance();
                BeanManager beanManager = Arc.container().beanManager();
                // Notify observers
                beanManager.getEvent().fire(resourceAdapter);
            }
            Log.tracef("Deploying JCA Resource Adapter: %s ", resourceAdapter);
            vertx.deployVerticle(new JCAVerticle(resourceAdapter));
        } catch (Exception e) {
            //FIXME: bubble up the exception
            Log.error("Cannot deploy", e);
        }
        return resourceAdapter;
    }
}
