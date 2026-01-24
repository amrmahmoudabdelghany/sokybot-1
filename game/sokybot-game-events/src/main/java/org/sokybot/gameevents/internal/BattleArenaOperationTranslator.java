package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.arena.BattleArenaOperationEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates battle arena operation (opcode 0x34D2).
 */
public class BattleArenaOperationTranslator extends AbstractTranslator {

    public BattleArenaOperationTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public BattleArenaOperationTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0x34D2;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            byte op = reader.getByte();
            Map<String, Object> data = new HashMap<>();
            data.put("operation", op);

            switch (op) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 0xD:
                case 0xE:
                case 0xF:
                    data.put("matchType", reader.getByte());
                    data.put("gameTypeMask", reader.getShort());
                    break;
                case 5:
                case 9:
                case 0xB:
                case 0xC:
                    data.put("matchType", reader.getByte());
                    if (op == 9) {
                        data.put("gameResult", reader.getByte());
                        data.put("coinCount", reader.getByte());
                        data.put("skillExp", reader.getInt());
                    }
                    break;
                case 8:
                    data.put("maxTime", reader.getInt());
                    break;
                case (byte) 0xFF:
                    byte update = reader.getByte();
                    data.put("updateType", update);
                    if (update == 0) {
                        data.put("requestType", reader.getByte());
                        data.put("gameType", reader.getByte());
                    } else if (update == 0x40) {
                        data.put("gainedPoints", reader.getInt());
                    } else if (update == 0x41) {
                        data.put("totalRed", reader.getInt());
                        data.put("totalBlue", reader.getInt());
                        int rankCount = reader.getByte() & 0xFF;
                        data.put("rankCount", rankCount);
                        // Simplified: not parsing full rank list into map for now
                    } else if (update == 0xF0) {
                        data.put("maxTime", reader.getInt());
                        data.put("elapsedTime", reader.getInt());
                    }
                    break;
            }

            return singleEvent(BattleArenaOperationEvent.builder()
                    .fullName(machineId)
                    .operation(op)
                    .data(data)
                    .build());

        } catch (Exception e) {
            return noEvents();
        }
    }
}
