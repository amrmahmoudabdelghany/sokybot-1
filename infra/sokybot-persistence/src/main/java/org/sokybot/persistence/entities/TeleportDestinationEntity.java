package org.sokybot.persistence.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class TeleportDestinationEntity {

    @Id
    private int refId;
    private float x;
    private float y;
    private float z;
    private long goldCost;

    public TeleportDestinationEntity() {
    }

    public int getRefId() {
        return refId;
    }

    public void setRefId(int refId) {
        this.refId = refId;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public long getGoldCost() {
        return goldCost;
    }

    public void setGoldCost(long goldCost) {
        this.goldCost = goldCost;
    }
}
