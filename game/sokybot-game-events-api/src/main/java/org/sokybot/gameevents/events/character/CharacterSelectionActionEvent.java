package org.sokybot.gameevents.events.character;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

import java.util.List;

@Getter
@Builder
@ToString
public class CharacterSelectionActionEvent implements IGameEvent {
    private final String fullName;
    private final long timestamp;

    private final byte action;
    private final byte result;
    private final List<CharSelectionEntry> characters;
    private final Integer errorCode;

    @Getter
    @Builder
    @ToString
    public static class CharSelectionEntry {
        private final int refObjId;
        private final String name;
        private final byte scale;
        private final byte level;
        private final long expOffset;
        private final int strength;
        private final int intelligence;
        private final int statPoint;
        private final int curHP;
        private final int curMP;
        private final boolean deleting;
        private final int deleteTime; // in minutes

        private final byte guildMemberClass;
        private final boolean guildRenameRequired;
        private final String curGuildName;
        private final byte academyMemberClass;

        private final List<SelectedCharacterItem> items;
        private final List<SelectedCharacterItem> avatarItems;
    }

    @Getter
    @Builder
    @ToString
    public static class SelectedCharacterItem {
        private final int refItemId;
        private final byte plus;
    }
}
