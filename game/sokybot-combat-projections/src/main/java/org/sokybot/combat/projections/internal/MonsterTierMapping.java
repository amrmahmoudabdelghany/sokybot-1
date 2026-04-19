package org.sokybot.combat.projections.internal;

import org.sokybot.combat.api.MonsterTier;
import org.sokybot.gameevents.enums.MonsterType;

/** Maps translator {@link MonsterType} to tactical {@link MonsterTier} for policy filters. */
final class MonsterTierMapping {

    private MonsterTierMapping() {
    }

    static MonsterTier from(MonsterType type) {
        if (type == null) {
            return MonsterTier.UNKNOWN;
        }
        switch (type) {
            case Normal:
                return MonsterTier.NORMAL;
            case Champion:
                return MonsterTier.CHAMPION;
            case Unique:
                return MonsterTier.UNIQUE;
            case Giant:
                return MonsterTier.GIANT;
            case Strong:
                return MonsterTier.TITAN;
            case Elite1:
            case Elite2:
            case Elite:
                return MonsterTier.ELITE;
            case Party:
            case PartyChampion:
            case PartyUnique:
            case PartyGiant:
            case PartyStrong:
                return MonsterTier.PARTY;
            case Event:
                return MonsterTier.EVENT;
            case Quest:
                return MonsterTier.QUEST;
            case UNKNOWN:
            default:
                return MonsterTier.UNKNOWN;
        }
    }
}
