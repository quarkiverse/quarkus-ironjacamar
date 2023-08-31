package io.quarkiverse.ironjacamar;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.PublisherDecorator;

@ApplicationScoped
public class AppPublisherDecorator implements PublisherDecorator {

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public Multi<? extends Message<?>> decorate(Multi<? extends Message<?>> publisher, String channelName,
            boolean isConnector) {
        if (isConnector) {
            AtomicLong counter = new AtomicLong();
            counters.put(channelName, counter);
            return publisher.onItem().invoke(counter::incrementAndGet);
        } else {
            return publisher;
        }
    }

    @Override
    public int getPriority() {
        return 10;
    }

    public long getMessageCount(String channel) {
        AtomicLong atomicLong = counters.get(channel);
        return atomicLong != null ? atomicLong.get() : 0L;
    }
}
