package org.sokybot.gamemodel.spec;

import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.dto.ItemData;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ItemSpec {
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
    int amount;
    byte slot;

    public static ItemSpec fromData(ItemData data, byte slot) {
        return ItemSpec.builder()
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
                .amount(data.getAmount())
                .slot(slot)
                .build();
    }
}
