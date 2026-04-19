package org.sokybot.town.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Vendor-specialized NPC handle (still valid when only catalogue data exists before spawn).
 */
public final class VendorRef {

    private final Integer entityUniqueId;
    private final int catalogueNpcRefId;
    private final String vendorCategoryKey;
    private final String displayName;
    private final NpcRole role;
    private final float worldX;
    private final float worldY;
    private final float worldZ;
    private final int regionId;

    private VendorRef(Builder builder) {
        this.entityUniqueId = builder.entityUniqueId;
        this.catalogueNpcRefId = builder.catalogueNpcRefId;
        this.vendorCategoryKey = Objects.requireNonNull(builder.vendorCategoryKey, "vendorCategoryKey");
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

    /** Distinct template key from bundled JSON or static tables (potions vs stable, …). */
    public String getVendorCategoryKey() {
        return vendorCategoryKey;
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

    /** Convenience adapter when a generic {@link NpcRef} must become a vendor handle. */
    public static VendorRef fromNpc(NpcRef npc, String vendorCategoryKey) {
        return VendorRef.builder()
                .entityUniqueId(npc.getEntityUniqueId().orElse(null))
                .catalogueNpcRefId(npc.getCatalogueNpcRefId())
                .vendorCategoryKey(vendorCategoryKey)
                .displayName(npc.getDisplayName())
                .role(npc.getRole())
                .worldPosition(npc.getWorldX(), npc.getWorldY(), npc.getWorldZ())
                .regionId(npc.getRegionId())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Integer entityUniqueId;
        private int catalogueNpcRefId;
        private String vendorCategoryKey = "";
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

        public Builder vendorCategoryKey(String vendorCategoryKey) {
            this.vendorCategoryKey = vendorCategoryKey;
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

        public VendorRef build() {
            return new VendorRef(this);
        }
    }
}
