package io.quarkiverse.ironjacamar.runtime;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.vertx.core.Vertx;

/**
 * Creates the {@link IronJacamarContainer} synthetic bean for a given resource adapter.
 */
public class IronJacamarContainerBeanCreator implements BeanCreator<IronJacamarContainer> {

    @Override
    public IronJacamarContainer create(SyntheticCreationalContext<IronJacamarContainer> context) {
        String id = (String) context.getParams().get("id");
        String kind = (String) context.getParams().get("kind");
        IronJacamarSupport containerProducer = context.getInjectedReference(IronJacamarSupport.class);
        Vertx vertx = context.getInjectedReference(Vertx.class);
        return containerProducer.createContainer(vertx, id, kind);
    }
}
