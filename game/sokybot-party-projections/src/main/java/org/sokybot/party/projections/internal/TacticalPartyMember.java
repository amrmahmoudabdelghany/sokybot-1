package org.sokybot.party.projections.internal;

import org.sokybot.party.api.PartyClass;

final class TacticalPartyMember {

    final int entityId;
    volatile String charName = "";
    volatile int level;
    volatile int hpPercent = -1;
    volatile int mpPercent = -1;
    volatile int currentHp;
    volatile int maxHp = 1;
    volatile int currentMp;
    volatile int maxMp = 1;
    volatile float x;
    volatile float y;
    volatile float z;
    volatile long lastUpdateEpochMs;
    volatile PartyClass partyClass = PartyClass.UNKNOWN;

    TacticalPartyMember(int entityId) {
        this.entityId = entityId;
    }
}
