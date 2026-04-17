package org.sokybot.http.server.events;

import java.util.function.Consumer;

import org.sokybot.commons.topic.Topic;

public interface IEventMediator {
    <T> void post(Topic topic, T payload);

    <T> void postScoped(String machineId, Topic topic, T payload);

    <T> IEventBridge.Subscription subscribe(Topic pattern, Consumer<BridgeEvent<T>> callback);

    default void post(String topic, Object payload) {
        post(Topic.parse(topic), payload);
    }

    default void postScoped(String machineId, String topic, Object payload) {
        postScoped(machineId, Topic.parse(topic), payload);
    }

    @SuppressWarnings("unchecked")
    default IEventBridge.Subscription subscribe(String pattern, Consumer<BridgeEvent<Object>> callback) {
        return subscribe(Topic.parse(pattern), (Consumer<BridgeEvent<Object>>) (Consumer<?>) callback);
    }
}
