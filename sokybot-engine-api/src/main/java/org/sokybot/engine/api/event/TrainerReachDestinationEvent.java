package org.sokybot.engine.api.event;

public class TrainerReachDestinationEvent {
    private final float x;
    private final float y;

    public TrainerReachDestinationEvent(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
