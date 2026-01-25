package org.sokybot.gameevents.events.character;

import org.sokybot.gameevents.events.core.IGameEvent;
import lombok.Getter;
import lombok.ToString;

/**
 * Event for character/guild rename response (opcode 0xB450).
 */
@Getter
@ToString
public class CharacterRenameAckEvent implements IGameEvent {
    private final String fullName;
    private final long timestamp;
    private final byte renameAction;
    private final byte result;
    private final int errorCode;

    public CharacterRenameAckEvent(String fullName, byte renameAction, byte result, int errorCode) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.renameAction = renameAction;
        this.result = result;
        this.errorCode = errorCode;
    }
}
