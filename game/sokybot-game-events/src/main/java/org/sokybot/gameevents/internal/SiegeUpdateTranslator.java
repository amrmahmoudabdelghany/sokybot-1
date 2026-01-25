package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.world.SiegeUpdateEvent;
import org.sokybot.gameevents.events.world.SiegeUpdateEvent.SiegeFortressInfo;
import org.sokybot.gameevents.events.world.SiegeUpdateEvent.SiegeStructureInfo;
import org.sokybot.gameevents.events.world.SiegeUpdateEvent.SiegeUpdateType;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.ArrayList;
import java.util.List;

public class SiegeUpdateTranslator extends AbstractTranslator {

    public SiegeUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public SiegeUpdateTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0x385F;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        long timestamp = System.currentTimeMillis();

        byte updateTypeByte = reader.getByte();
        SiegeUpdateType type = SiegeUpdateType.fromValue(updateTypeByte);

        SiegeUpdateEvent.SiegeUpdateEventBuilder builder = SiegeUpdateEvent.builder()
                .fullName(machineId)
                .timestamp(timestamp)
                .type(type);

        switch (type) {
            case INFO:
                byte fortressCount = reader.getByte();
                List<SiegeFortressInfo> fortresses = new ArrayList<>();
                for (int i = 0; i < fortressCount; i++) {
                    int id = reader.getInt();
                    String guildName = reader.getString();
                    int guildId = reader.getInt();
                    String leaderName = reader.getString();
                    String instruction = reader.getString();
                    int guildCrestRev = reader.getInt();
                    int unionId = reader.getInt();
                    int unionCrestRev = reader.getInt();
                    boolean enterEnabled = reader.getByte() != 0;
                    int enterCD = enterEnabled ? reader.getInt() : 0;
                    boolean stoneEnabled = reader.getByte() != 0;
                    int stoneCD = stoneEnabled ? reader.getInt() : 0;

                    fortresses.add(SiegeFortressInfo.builder()
                            .id(id)
                            .guildName(guildName)
                            .guildId(guildId)
                            .leaderName(leaderName)
                            .instruction(instruction)
                            .guildCrestRev(guildCrestRev)
                            .unionId(unionId)
                            .unionCrestRev(unionCrestRev)
                            .enterCountdownEnabled(enterEnabled)
                            .enterCountdown(enterCD)
                            .stoneCooldownEnabled(stoneEnabled)
                            .stoneCooldown(stoneCD)
                            .build());
                }
                builder.fortresses(fortresses);
                builder.siegePeriod(reader.getByte());
                builder.owningFortressID(reader.getInt());
                break;

            case TAX_RATE:
                builder.siegeId(reader.getInt());
                builder.taxRate(reader.getShort());
                break;

            case OCCUPIED:
                builder.siegeId(reader.getInt());
                builder.guildName(reader.getString());
                builder.leaderName(reader.getString());
                builder.instruction(reader.getString());
                builder.guildId(reader.getInt());
                builder.guildCrestRev(reader.getInt());
                builder.unionId(reader.getInt());
                builder.unionCrestRev(reader.getInt());
                break;

            case SEAL_DESTROYED:
                builder.siegeId(reader.getInt());
                break;

            case STRUCTURE_STATE:
                builder.siegeId(reader.getInt());
                builder.structureUniqueID(reader.getInt());
                builder.refEventStructID(reader.getInt());
                builder.structureState(reader.getShort());
                // Optional guild name if it's headquarter
                try {
                    builder.guildName(reader.getString());
                } catch (Exception e) {
                }
                break;

            case BATTLE_RANK:
                boolean self = reader.getByte() != 0;
                builder.self(self);
                if (!self) {
                    builder.playerName(reader.getString());
                }
                builder.rankLevel(reader.getByte());
                break;

            case STRUCTURE_INFO:
                builder.siegeId(reader.getInt());
                byte strucCount = reader.getByte();
                List<SiegeStructureInfo> structureInfos = new ArrayList<>();
                for (int i = 0; i < strucCount; i++) {
                    int refEventStructID = reader.getInt();
                    int refObjID = reader.getInt();
                    int curHP = reader.getInt();
                    short state = reader.getShort();
                    boolean occupied = reader.getByte() != 0;
                    String occGuild = occupied ? reader.getString() : null;

                    structureInfos.add(SiegeStructureInfo.builder()
                            .refEventStructID(refEventStructID)
                            .refObjID(refObjID)
                            .curHP(curHP)
                            .state(state)
                            .occupied(occupied)
                            .occupyingGuildName(occGuild)
                            .build());
                }
                builder.structures(structureInfos);
                break;

            case BATTLE_RECORD:
                builder.siegeId(reader.getInt());
                builder.killCount(reader.getInt());
                builder.killedCount(reader.getInt());
                break;

            default:
                break;
        }

        return List.of(builder.build());
    }
}
