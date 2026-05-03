package org.sokybot.warroom.domain;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;

/**
 * Problem fact: one party slot / bucket (max {@link #capacity} bots).
 */
public class WarRoomParty {

    @PlanningId
    private String id;

    private int capacity = 8;

    public WarRoomParty() {
    }

    public WarRoomParty(String id, int capacity) {
        this.id = id;
        this.capacity = capacity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return id;
    }
}
