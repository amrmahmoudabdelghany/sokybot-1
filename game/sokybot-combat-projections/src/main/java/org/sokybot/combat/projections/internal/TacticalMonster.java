package org.sokybot.combat.projections.internal;

import org.sokybot.gameevents.enums.MonsterType;

final class TacticalMonster {

    final int entityId;
    volatile int refObjId;
    volatile int levelOrZero;
    volatile int currentHp;
    volatile int maxHp;
    volatile float x;
    volatile float y;
    volatile float z;
    volatile MonsterType monsterType;
    volatile boolean aggressiveTowardSelf = false;

    TacticalMonster(int entityId) {
        this.entityId = entityId;
    }

    boolean championOrUnique() {
        MonsterType t = monsterType;
        return t != null && (t == MonsterType.Champion || t == MonsterType.Unique || t == MonsterType.PartyChampion
                || t == MonsterType.PartyUnique);
    }
}
