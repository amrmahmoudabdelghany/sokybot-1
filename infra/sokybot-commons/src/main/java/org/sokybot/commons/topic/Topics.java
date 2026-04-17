package org.sokybot.commons.topic;

public final class Topics {
    private Topics() {
    }

    public static Topic game(String machineId, String eventSimpleName) {
        return Topic.game(machineId, eventSimpleName);
    }

    public static Topic network(String machineId, String transition) {
        return Topic.network(machineId, transition);
    }

    public static Topic party(String groupName, String eventType) {
        return Topic.party(groupName, eventType);
    }
}
