package org.sokybot.gameevents.internal;

import java.util.ArrayList;
import java.util.List;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.entity.EntitySpawnEvent;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates group spawn data packets (opcode 0x3019).
 */
public class GroupSpawnDataTranslator extends AbstractTranslator {
    public GroupSpawnDataTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public GroupSpawnDataTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0x3019;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            int count = reader.getShort() & 0xFFFF;
            List<IGameEvent> events = new ArrayList<>();
            SpawnDataReader dataReader = new SpawnDataReader(reader, lookup);

            for (int i = 0; i < count; i++) {
                int refId = reader.getInt();
                lookup.findNPC(refId).ifPresent(entity -> {
                    var data = dataReader.readMonster(entity);
                    events.add(new EntitySpawnEvent(machineFullName,
                            data.getUniqueId(), data.getRefId(), data.getName(),
                            data.getXSector(), data.getYSector(),
                            data.getXOffset(), data.getYOffset(), data.getZOffset(),
                            data.getAngle(), data.getPosition()));
                });
            }

            return events;
        } catch (Exception e) {
            return noEvents();
        }
    }
}
