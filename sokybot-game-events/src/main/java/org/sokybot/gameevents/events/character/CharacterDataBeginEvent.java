package org.sokybot.gameevents.events.character;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when character data loading transaction begins (opcode 0x34A5).
 * Signals the start of a chunked packet sequence that will culminate in
 * CharacterLoadedEvent after all data is accumulated and parsed.
 */
public class CharacterDataBeginEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    public CharacterDataBeginEvent(String fullName) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "CharacterDataBeginEvent{fullName='" + fullName + "'}";
    }
}

