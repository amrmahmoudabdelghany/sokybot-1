package org.sokybot.commons.topic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sokybot.commons.osgi.OsgiEventTopics;

class TopicTest {
    @Test
    void shouldKeepSanitizationParityWithOsgiEventTopics() {
        String machineId = "Group.Machine 1";
        String event = "AgentListEvent";
        assertEquals(
                OsgiEventTopics.gameTopic(machineId, event),
                Topic.game(machineId, event).toEventAdminString());
        assertEquals(
                OsgiEventTopics.networkTopic(machineId, "Connected"),
                Topic.network(machineId, "Connected").toEventAdminString());
    }
}
