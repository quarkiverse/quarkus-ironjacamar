package io.quarkiverse.jca.runtime;

import java.util.Map;
import java.util.function.Supplier;

import jakarta.resource.spi.ResourceAdapter;

import io.quarkiverse.jca.runtime.vertx.JCAVerticle;
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
            resourceAdapter = (ResourceAdapter) Class.forName(resourceAdapterClassName).getDeclaredConstructor()
                    .newInstance();

            //FIXME: Apply the config properties
            Log.tracef("Deploying JCA Resource Adapter: %s ", resourceAdapter);
            vertx.deployVerticle(new JCAVerticle(resourceAdapter));
        } catch (Exception e) {
            //FIXME: bubble up the exception
            Log.error("Cannot deploy", e);
        }
        return resourceAdapter;
    }
}
