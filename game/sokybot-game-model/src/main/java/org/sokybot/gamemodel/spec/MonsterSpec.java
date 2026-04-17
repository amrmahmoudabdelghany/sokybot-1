package org.sokybot.gamemodel.spec;

import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.dto.MonsterData;
import org.sokybot.gameevents.enums.MonsterType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MonsterSpec {
    int uniqueId;
    int refId;
    String name;
    int xSector;
    int ySector;
    float xOffset;
    float yOffset;
    float zOffset;
    short angle;
    GamePosition position;
    MonsterType monsterType;
    int level;
    int currentHp;
    int maxHp;

    public static MonsterSpec fromData(MonsterData data) {
        return MonsterSpec.builder()
                .uniqueId(data.getUniqueId())
                .refId(data.getRefId())
                .name(data.getName())
                .xSector(data.getXSector())
                .ySector(data.getYSector())
                .xOffset(data.getXOffset())
                .yOffset(data.getYOffset())
                .zOffset(data.getZOffset())
                .angle(data.getAngle())
                .position(data.getPosition())
                .monsterType(data.getMonsterType())
                .level(data.getLevel())
                .currentHp(data.getCurrentHp())
                .maxHp(data.getMaxHp())
                .build();
    }
}
