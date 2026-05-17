package io.quarkiverse.ironjacamar.reactive.messaging;

/**
 * Metadata attached to incoming Reactive Messaging messages originating from a JCA resource adapter.
 *
 * @param channelName the Reactive Messaging channel name
 * @param resourceAdapterKind the resource adapter kind (e.g., "artemis")
 */
public record IronJacamarIncomingMetadata(String channelName, String resourceAdapterKind) {
}
