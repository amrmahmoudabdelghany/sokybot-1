package org.sokybot.gamemodel.spec;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TrainerSpec {
    int uniqueId;
    int refId;
    String name;
    int xSector;
    int ySector;
    float xOffset;
    float yOffset;
    float zOffset;
    short angle;

    public static TrainerSpec empty() {
        return TrainerSpec.builder()
                .uniqueId(0)
                .refId(0)
                .name("None")
                .xSector(0)
                .ySector(0)
                .xOffset(0f)
                .yOffset(0f)
                .zOffset(0f)
                .angle((short) 0)
                .build();
    }
}
