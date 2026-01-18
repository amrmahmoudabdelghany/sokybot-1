package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.entity.EntitySpawnEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.entities.navmesh.Position;
import org.sokybot.persistence.service.IGameDataLookup;

public class EntitySpawnTranslator extends AbstractTranslator {
    
    private static final int ENTITY_SPAWN_OPCODE = 0x3015;
    public EntitySpawnTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return ENTITY_SPAWN_OPCODE;
    }
    
    @Override
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            int refId = reader.getInt();
            
            SpawnDataReader dataReader = new SpawnDataReader(reader, lookup);
            
            var itemOpt = lookup.findItem(refId);
            if (itemOpt.isPresent()) {
                var itemEntity = itemOpt.get();
                var itemData = dataReader.readDropItem(itemEntity);
                return singleEvent(new org.sokybot.gameevents.events.spawn.ItemSpawnEvent(machineFullName, itemData));
            }

             var npcOpt = lookup.findNPC(refId);
             if (npcOpt.isPresent()) {
                 var npc = npcOpt.get();
                 var type = npc.getType();
                 var longId = npc.getLongId();
                 
                 // Player
                 if (type.name().startsWith("Player")) { 
                      return noEvents();
                 }
                 // Monster
                 if (longId.contains("MOB_")) {
                      var monsterData = dataReader.readMonster(npc);
                      return singleEvent(new org.sokybot.gameevents.events.spawn.MonsterSpawnEvent(machineFullName, monsterData));
                 }
             }
            return noEvents();
        } catch (Exception e) {
            return noEvents();
        }
    }
}
