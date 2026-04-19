package org.sokybot.combat.projections.internal;

final class TacticalLoot {

    final int entityId;
    volatile int itemRefId;
    volatile Integer ownerEntityId;
    volatile long ownerExpiresAtEpochMs;
    volatile float x;
    volatile float y;
    volatile float z;

    TacticalLoot(int entityId) {
        this.entityId = entityId;
    }
}
