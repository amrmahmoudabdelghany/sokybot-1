package org.sokybot.gamemodel.spec;

import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.dto.PlayerData;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlayerSpec {
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
    String guildName;

    public static PlayerSpec fromData(PlayerData data) {
        return PlayerSpec.builder()
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
                .guildName(data.getGuildName())
                .build();
    }
}
