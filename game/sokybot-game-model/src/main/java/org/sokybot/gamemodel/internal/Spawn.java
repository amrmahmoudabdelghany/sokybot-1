package org.sokybot.gamemodel.internal;

import java.util.Objects;

import org.sokybot.gameevents.dto.SpawnData;
import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.gameevents.dto.GamePosition;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode
@ToString
public abstract class Spawn implements ISpawn {

    private int uniqueId;
    private int refId;
    private String name;

    private int xSector;
    private int ySector;
    private float xOffset;
    private float yOffset;
    private float zOffset;
    private short angle;

    private GamePosition position;

    // World coordinates derived/stored
    private int x;
    private int y;

    public Spawn(SpawnData data) {
        if (data != null) {
            this.uniqueId = data.getUniqueId();
            this.refId = data.getRefId();
            this.name = data.getName();
            this.xSector = data.getXSector();
            this.ySector = data.getYSector();
            this.xOffset = data.getXOffset();
            this.yOffset = data.getYOffset();
            this.zOffset = data.getZOffset();
            this.angle = data.getAngle();
            this.position = data.getPosition();
        }
    }

    // Derived methods

    @Override
    public double distance(int tx, int ty) {
        return Math.sqrt(Math.pow(this.x - tx, 2) + Math.pow(this.y - ty, 2));
    }

    @Override
    public double distance(float tx, float ty) {
        return Math.sqrt(Math.pow(this.x - tx, 2) + Math.pow(this.y - ty, 2));
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
