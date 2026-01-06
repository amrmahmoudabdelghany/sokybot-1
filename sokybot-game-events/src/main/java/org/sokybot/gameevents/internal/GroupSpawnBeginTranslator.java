package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.GroupSpawnBeginEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates group spawn begin packets (opcode 0x3019) to GroupSpawnBeginEvent.
 * Based on EnvironmentHandler.onGroupSpawnBegin() pattern.
 */
@Component(service = IPacketTranslator.class)
public class GroupSpawnBeginTranslator implements IPacketTranslator {
    
    private static final int GROUP_SPAWN_BEGIN_OPCODE = 0x3019;
    
    @Override
    public int getOpcode() {
        return GROUP_SPAWN_BEGIN_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte spawnType = reader.getByte();  // 1 = spawn, 2 = despawn
            short count = reader.getShort();
            
            return new GroupSpawnBeginEvent(machineFullName, spawnType, count);
            
        } catch (Exception e) {
            return null;
        }
    }
}
