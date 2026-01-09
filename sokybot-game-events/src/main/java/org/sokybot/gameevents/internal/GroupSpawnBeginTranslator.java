package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.entity.GroupSpawnBeginEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates group spawn begin packets (opcode 0x3019) to GroupSpawnBeginEvent.
 * Based on EnvironmentHandler.onGroupSpawnBegin() pattern.
 */
public class GroupSpawnBeginTranslator extends AbstractTranslator {
    
    private static final int GROUP_SPAWN_BEGIN_OPCODE = 0x3019;
    
    public GroupSpawnBeginTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return GROUP_SPAWN_BEGIN_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte spawnType = reader.getByte();  // 1 = spawn, 2 = despawn
            short count = reader.getShort();
            
            return singleEvent(new GroupSpawnBeginEvent(machineFullName, spawnType, count));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
