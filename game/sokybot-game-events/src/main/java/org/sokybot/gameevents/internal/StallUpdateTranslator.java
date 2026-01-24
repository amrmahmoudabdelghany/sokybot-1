package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.stall.StallUpdateEvent;
import org.sokybot.gameevents.events.stall.StallUpdateEvent.StallUpdateType;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.List;

public class StallUpdateTranslator extends AbstractTranslator {

    public StallUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public StallUpdateTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0xB0BA;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        long timestamp = System.currentTimeMillis();

        byte result = reader.getByte();
        byte typeRaw = reader.getByte();
        StallUpdateType type = mapUpdateType(typeRaw);

        StallUpdateEvent.StallUpdateEventBuilder builder = StallUpdateEvent.builder()
                .fullName(machineId)
                .timestamp(timestamp)
                .result(result)
                .type(type);

        switch (type) {
            case UPDATE_ITEM:
                builder.slot(reader.getByte());
                builder.stackCount((int) reader.getShort());
                builder.price(reader.getLong());
                builder.errorCode((int) reader.getShort());
                break;
            case ADD_ITEM:
            case REMOVE_ITEM:
                builder.errorCode((int) reader.getShort());
                // Further parsing would require genericItemData which is complex.
                // For now, we just track the result and error code.
                break;
            case STATE:
                builder.isOpen(reader.getBoolean());
                builder.errorCode((int) reader.getShort()); // stallNetworkResult
                break;
            case MESSAGE:
                builder.message(reader.getString());
                break;
            case NAME:
                // Typically name is in 0x30BB, but documentation mentions it here too.
                builder.name(reader.getString());
                break;
            case UNKNOWN:
                break;
        }

        return List.of(builder.build());
    }

    private StallUpdateType mapUpdateType(byte type) {
        switch (type) {
            case 1:
                return StallUpdateType.UPDATE_ITEM;
            case 2:
                return StallUpdateType.ADD_ITEM;
            case 3:
                return StallUpdateType.REMOVE_ITEM;
            case 5:
                return StallUpdateType.STATE;
            case 6:
                return StallUpdateType.MESSAGE;
            case 7:
                return StallUpdateType.NAME;
            default:
                return StallUpdateType.UNKNOWN;
        }
    }
}
