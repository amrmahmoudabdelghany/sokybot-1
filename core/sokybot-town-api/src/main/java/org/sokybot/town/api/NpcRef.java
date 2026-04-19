package org.sokybot.town.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Runtime or catalogue reference to an NPC entity the town loop may path to and interact with.
 */
public final class NpcRef {

    private final Integer entityUniqueId;
    private final int catalogueNpcRefId;
    private final String displayName;
    private final NpcRole role;
    private final float worldX;
    private final float worldY;
    private final float worldZ;
    private final int regionId;

    private NpcRef(Builder builder) {
        this.entityUniqueId = builder.entityUniqueId;
        this.catalogueNpcRefId = builder.catalogueNpcRefId;
        this.displayName = Objects.requireNonNull(builder.displayName, "displayName");
        this.role = Objects.requireNonNull(builder.role, "role");
        this.worldX = builder.worldX;
        this.worldY = builder.worldY;
        this.worldZ = builder.worldZ;
        this.regionId = builder.regionId;
    }

    public Optional<Integer> getEntityUniqueId() {
        return Optional.ofNullable(entityUniqueId);
    }

    public int getCatalogueNpcRefId() {
        return catalogueNpcRefId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public NpcRole getRole() {
        return role;
    }

    public float getWorldX() {
        return worldX;
    }

    public float getWorldY() {
        return worldY;
    }

    public float getWorldZ() {
        return worldZ;
    }

    public int getRegionId() {
        return regionId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Integer entityUniqueId;
        private int catalogueNpcRefId;
        private String displayName = "";
        private NpcRole role = NpcRole.UNKNOWN;
        private float worldX;
        private float worldY;
        private float worldZ;
        private int regionId;

        public Builder entityUniqueId(Integer entityUniqueId) {
            this.entityUniqueId = entityUniqueId;
            return this;
        }

        public Builder catalogueNpcRefId(int catalogueNpcRefId) {
            this.catalogueNpcRefId = catalogueNpcRefId;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder role(NpcRole role) {
            this.role = role;
            return this;
        }

        public Builder worldPosition(float x, float y, float z) {
            this.worldX = x;
            this.worldY = y;
            this.worldZ = z;
            return this;
        }

        public Builder regionId(int regionId) {
            this.regionId = regionId;
            return this;
        }

        public NpcRef build() {
            return new NpcRef(this);
        }
    }
}
