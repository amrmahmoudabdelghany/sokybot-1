package org.sokybot.navigation.api;

import java.util.Objects;

/**
 * Immutable 3D point in world space (game X / Y / Z plane order as used by the client model).
 */
public final class WorldPoint {

    private final float x;
    private final float y;
    private final float z;

    public WorldPoint(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float distanceTo(WorldPoint other) {
        if (other == null) {
            return Float.POSITIVE_INFINITY;
        }
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        float dz = this.z - other.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorldPoint)) {
            return false;
        }
        WorldPoint worldPoint = (WorldPoint) o;
        return Float.compare(worldPoint.x, x) == 0
                && Float.compare(worldPoint.y, y) == 0
                && Float.compare(worldPoint.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Float.valueOf(x), Float.valueOf(y), Float.valueOf(z));
    }
}
