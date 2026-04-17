package org.sokybot.gamemodel.factory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.enums.MonsterType;
import org.sokybot.gamemodel.internal.DefaultEntityFactory;
import org.sokybot.gamemodel.spec.MonsterSpec;
import org.sokybot.gamemodel.spec.TrainerSpec;

class DefaultEntityFactoryTest {

    @Test
    void shouldCreateLiveTrainerAndMonsterInstances() {
        DefaultEntityFactory factory = new DefaultEntityFactory();
        assertNotNull(factory.createTrainer(TrainerSpec.empty()));
        assertNotNull(factory.createMonster(MonsterSpec.builder()
                .uniqueId(1).refId(2).name("M").xSector(0).ySector(0)
                .xOffset(0).yOffset(0).zOffset(0).angle((short) 0)
                .position(new GamePosition(0f, 0f, 0f, (byte) 0)).monsterType(MonsterType.Normal)
                .level(1).currentHp(1).maxHp(1).build()));
    }
}
