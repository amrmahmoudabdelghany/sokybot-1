package org.sokybot.gameevents.events.community;

import org.sokybot.gameevents.events.core.IGameEvent;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Event containing the list of friends and groups (opcode 0x3305).
 */
@Getter
@ToString
public class FriendListInfoEvent implements IGameEvent {
    private final String fullName;
    private final long timestamp;
    private final List<FriendGroup> groups;
    private final List<FriendEntry> friends;

    public FriendListInfoEvent(String fullName, List<FriendGroup> groups, List<FriendEntry> friends) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.groups = groups;
        this.friends = friends;
    }

    @Getter
    @Builder
    @ToString
    public static class FriendGroup {
        private final int id;
        private final String name;
    }

    @Getter
    @Builder
    @ToString
    public static class FriendEntry {
        private final int charId;
        private final String name;
        private final int modelId;
        private final int groupId;
        private final boolean offline;
    }
}
