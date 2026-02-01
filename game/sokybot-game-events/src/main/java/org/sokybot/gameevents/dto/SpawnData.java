package org.sokybot.gameevents.dto;

import org.sokybot.gameevents.dto.GamePosition;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

@Getter
@SuperBuilder
@ToString
@EqualsAndHashCode
public class SpawnData {

    private final int uniqueId;
    private final int refId;
    private final String name;

    private final int xSector;
    private final int ySector;
    private final float xOffset;
    private final float yOffset;
    private final float zOffset;
    private final short angle;

    private final GamePosition position;
}
