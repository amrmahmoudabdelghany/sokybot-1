package org.sokybot.topology.api;

import java.util.Objects;

import org.sokybot.navigation.api.WorldPoint;

public final class TeleportNode {

    private final int npcRefId;
    private final String name;
    private final WorldPoint position;
    private final int packedSector;

    public TeleportNode(int npcRefId, String name, WorldPoint position, int packedSector) {
        this.npcRefId = npcRefId;
        this.name = Objects.requireNonNull(name, "name");
        this.position = Objects.requireNonNull(position, "position");
        this.packedSector = packedSector;
    }

    public int getNpcRefId() {
        return npcRefId;
    }

    public String getName() {
        return name;
    }

    public WorldPoint getPosition() {
        return position;
    }

    public int getPackedSector() {
        return packedSector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TeleportNode)) {
            return false;
        }
        TeleportNode that = (TeleportNode) o;
        return npcRefId == that.npcRefId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(npcRefId);
    }
}
