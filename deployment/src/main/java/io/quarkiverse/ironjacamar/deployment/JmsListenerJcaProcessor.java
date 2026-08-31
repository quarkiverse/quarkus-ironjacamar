package io.quarkiverse.ironjacamar.deployment;

import java.lang.reflect.Modifier;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkiverse.ironjacamar.ResourceEndpoint;
import io.quarkiverse.jms.spi.deployment.JmsListenerDiscoveredBuildItem;
import io.quarkiverse.jms.spi.deployment.JmsListenerManagedBuildItem;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 * Consumes {@link JmsListenerDiscoveredBuildItem} from the Quarkus JMS extension
 * and generates thin {@link ResourceEndpoint} adapter classes that delegate to
 * the JMS extension's generated invoker, enabling JCA-based message delivery.
 */
class JmsListenerJcaProcessor {

    private static final Logger LOG = Logger.getLogger(JmsListenerJcaProcessor.class);

    private static final String MESSAGE_LISTENER_CLASS = "jakarta.jms.MessageListener";
    private static final String JMS_LISTENER_INVOKER_CLASS = "io.quarkiverse.jms.runtime.JmsListenerInvoker";

    @BuildStep
    void handleJmsListeners(
            CombinedIndexBuildItem combinedIndex,
            List<JmsListenerDiscoveredBuildItem> listeners,
            BuildProducer<JmsListenerManagedBuildItem> managed,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<ResourceEndpointBuildItem> resourceEndpoints,
            BuildProducer<ReflectiveClassBuildItem> reflective,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> configDefaults) {

        if (listeners.isEmpty()) {
            return;
        }

        IndexView index = combinedIndex.getIndex();

        ClassOutput classOutput = (name, data) -> {
            generatedBeans.produce(new GeneratedBeanBuildItem(name, data));
        };

        for (JmsListenerDiscoveredBuildItem listener : listeners) {
            if (!isJcaManaged(index, listener)) {
                continue;
            }

            if (listener.isRunOnVirtualThread()) {
                throw new RuntimeException(
                        "@RunOnVirtualThread is not supported with IronJacamar — XA transactions require " +
                                "processing on the resource adapter's thread. Remove @RunOnVirtualThread from " +
                                listener.getBeanClassName() + "." + listener.getMethodName());
            }

            String activationSpecKey = generateActivationSpecKey(listener);
            String endpointClassName = generateResourceEndpointClass(classOutput, listener, activationSpecKey);
            generateActivationSpecDefaults(configDefaults, listener, activationSpecKey);

            reflective.produce(ReflectiveClassBuildItem.builder(endpointClassName)
                    .constructors(true).methods(true).build());

            resourceEndpoints.produce(new ResourceEndpointBuildItem(
                    endpointClassName, activationSpecKey));

            managed.produce(new JmsListenerManagedBuildItem(
                    listener.getBeanClassName(), listener.getMethodName()));

            LOG.debugf("Generated @ResourceEndpoint for %s.%s with activation spec key '%s'",
                    listener.getBeanClassName(), listener.getMethodName(), activationSpecKey);
        }
    }

    private boolean isJcaManaged(IndexView index, JmsListenerDiscoveredBuildItem listener) {
        ClassInfo classInfo = index.getClassByName(listener.getBeanClassName());
        return classInfo != null && classInfo.hasAnnotation(ResourceEndpoint.class);
    }

    private String generateActivationSpecKey(JmsListenerDiscoveredBuildItem listener) {
        return "jms-listener-" + listener.getDestination()
                .replaceAll("[^a-zA-Z0-9-]", "-")
                .toLowerCase();
    }

    private String generateResourceEndpointClass(ClassOutput classOutput,
            JmsListenerDiscoveredBuildItem listener, String activationSpecKey) {

        String endpointClassName = listener.getBeanClassName().replace('$', '_') + "_" +
                listener.getMethodName() + "_ResourceEndpoint";
        String invokerClassName = listener.getInvokerClassName();

        try (ClassCreator creator = ClassCreator.builder()
                .classOutput(classOutput)
                .className(endpointClassName)
                .interfaces(MESSAGE_LISTENER_CLASS)
                .build()) {

            creator.addAnnotation(ApplicationScoped.class);
            creator.addAnnotation(Unremovable.class);
            creator.addAnnotation(ResourceEndpoint.class).addValue("activationSpecConfigKey", activationSpecKey);

            FieldDescriptor invokerField = creator.getFieldCreator("invoker", JMS_LISTENER_INVOKER_CLASS)
                    .setModifiers(Modifier.PRIVATE)
                    .getFieldDescriptor();

            // Constructor — instantiate the invoker (its constructor resolves beans from Arc)
            try (MethodCreator ctor = creator.getMethodCreator("<init>", void.class)) {
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
                ResultHandle invoker = ctor.newInstance(MethodDescriptor.ofConstructor(invokerClassName));
                ctor.writeInstanceField(invokerField, ctor.getThis(), invoker);
                ctor.returnVoid();
            }

            // onMessage — delegate to invoker.invoke(message, null)
            try (MethodCreator onMessage = creator.getMethodCreator("onMessage", void.class, Message.class)) {
                ResultHandle invoker = onMessage.readInstanceField(invokerField, onMessage.getThis());
                ResultHandle message = onMessage.getMethodParam(0);
                ResultHandle nullContext = onMessage.loadNull();
                onMessage.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(JMS_LISTENER_INVOKER_CLASS, "invoke",
                                void.class, Message.class, JMSContext.class),
                        invoker, message, nullContext);
                onMessage.returnVoid();
            }
        }

        return endpointClassName;
    }

    private void generateActivationSpecDefaults(
            BuildProducer<RunTimeConfigurationDefaultBuildItem> configDefaults,
            JmsListenerDiscoveredBuildItem listener, String activationSpecKey) {

        String prefix = "quarkus.ironjacamar.activation-spec." + activationSpecKey + ".config.";

        configDefaults.produce(new RunTimeConfigurationDefaultBuildItem(
                prefix + "destination", listener.getDestination()));
        configDefaults.produce(new RunTimeConfigurationDefaultBuildItem(
                prefix + "destination-type",
                listener.isTopic() ? "jakarta.jms.Topic" : "jakarta.jms.Queue"));

        if (listener.getConcurrency() > 1) {
            configDefaults.produce(new RunTimeConfigurationDefaultBuildItem(
                    prefix + "max-session", String.valueOf(listener.getConcurrency())));
        }
        if (listener.getSelector() != null && !listener.getSelector().isEmpty()) {
            configDefaults.produce(new RunTimeConfigurationDefaultBuildItem(
                    prefix + "message-selector", listener.getSelector()));
        }
        if (listener.getSubscription() != null && !listener.getSubscription().isEmpty()) {
            configDefaults.produce(new RunTimeConfigurationDefaultBuildItem(
                    prefix + "subscription-name", listener.getSubscription()));
            configDefaults.produce(new RunTimeConfigurationDefaultBuildItem(
                    prefix + "subscription-durability", "Durable"));
        }
    }
}
