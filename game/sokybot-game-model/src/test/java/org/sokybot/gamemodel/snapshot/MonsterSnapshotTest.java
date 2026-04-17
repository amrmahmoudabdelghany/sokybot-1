package org.sokybot.gamemodel.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.dto.MonsterData;
import org.sokybot.gameevents.enums.MonsterType;
import org.sokybot.gamemodel.internal.Monster;
import org.sokybot.gamemodel.internal.snapshot.MonsterSnapshot;

class MonsterSnapshotTest {

    @Test
    void shouldCaptureImmutableStateFromLiveMonster() {
        MonsterData data = MonsterData.builder()
                .uniqueId(10)
                .refId(20)
                .name("Tiger")
                .xSector(1)
                .ySector(2)
                .xOffset(3f)
                .yOffset(4f)
                .zOffset(5f)
                .angle((short) 10)
                .position(new GamePosition(3f, 4f, 5f, (byte) 0))
                .monsterType(MonsterType.Normal)
                .level(11)
                .currentHp(200)
                .maxHp(300)
                .build();
        Monster live = new Monster(data);
        MonsterSnapshot snapshot = MonsterSnapshot.of(live);

        live.setCurrentHP(1);
        live.setLevel(99);

        assertEquals(200, snapshot.getCurrentHP());
        assertEquals(11, snapshot.getLevel());
    }
}
