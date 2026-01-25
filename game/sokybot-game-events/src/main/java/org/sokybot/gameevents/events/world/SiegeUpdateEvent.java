package org.sokybot.gameevents.events.world;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

import java.util.List;

@Getter
@Builder
@ToString
public class SiegeUpdateEvent implements IGameEvent {

    public enum SiegeUpdateType {
        INFO(0),
        TAX_RATE(1),
        OCCUPIED(2),
        SEAL_DESTROYED(3),
        STRUCTURE_STATE(4),
        APPLICATION_APPLY(5),
        APPLICATION_CANCEL(6),
        BATTLE_RANK(7),
        STRUCTURE_INFO(8),
        DEFENDING_GUILDS(9),
        BATTLE_RECORD(10),
        EMPLOYED_NPC(11),
        REWARD2(12),
        UNKNOWN(-1);

        private final int value;

        SiegeUpdateType(int value) {
            this.value = value;
        }

        public static SiegeUpdateType fromValue(int value) {
            for (SiegeUpdateType type : values()) {
                if (type.value == value)
                    return type;
            }
            return UNKNOWN;
        }
    }

    private final String fullName;
    private final long timestamp;

    private final SiegeUpdateType type;

    // Type 0: Info
    private final List<SiegeFortressInfo> fortresses;
    private final byte siegePeriod;
    private final int owningFortressID;

    // Shared per-fortress updates
    private final Integer siegeId;

    // Type 1: TaxRate
    private final Short taxRate;

    // Type 2: Occupied
    private final String guildName;
    private final Integer guildId;
    private final String leaderName;
    private final String instruction;
    private final Integer guildCrestRev;
    private final Integer unionId;
    private final Integer unionCrestRev;

    // Type 4: StructureState
    private final Integer structureUniqueID;
    private final Integer refEventStructID;
    private final Short structureState;

    // Type 7: BattleRank
    private final Boolean self;
    private final String playerName;
    private final Byte rankLevel;

    // Type 8: StructureInfo
    private final List<SiegeStructureInfo> structures;

    // Type 10: BattleRecord
    private final Integer killCount;
    private final Integer killedCount;

    @Getter
    @Builder
    @ToString
    public static class SiegeFortressInfo {
        private final int id;
        private final String guildName;
        private final int guildId;
        private final String leaderName;
        private final String instruction;
        private final int guildCrestRev;
        private final int unionId;
        private final int unionCrestRev;
        private final boolean enterCountdownEnabled;
        private final int enterCountdown;
        private final boolean stoneCooldownEnabled;
        private final int stoneCooldown;
    }

    @Getter
    @Builder
    @ToString
    public static class SiegeStructureInfo {
        private final int refEventStructID;
        private final int refObjID;
        private final int curHP;
        private final short state;
        private final boolean occupied;
        private final String occupyingGuildName;
    }
}
