package org.sokybot.pk2extractor.dto.region;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sector/region navigation data extracted from .nvm files.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorRefData {
    
    private short sectorYX;
    
    @Builder.Default
    private List<NavObjectRefData> navObjects = new ArrayList<>();
    
    @Builder.Default
    private List<NavCellRefData> navCells = new ArrayList<>();
    
    @Builder.Default
    private List<NavBorderRefData> navBorders = new ArrayList<>();
    
    @Builder.Default
    private List<NavCellLinkRefData> navCellLinks = new ArrayList<>();
    
    /**
     * Object placement in sector.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavObjectRefData {
        private int objectId;
        private float posX;
        private float posY;
        private float posZ;
        private short collisionFlag;
        private float yaw;
        private short uniqueId;
        private short scale;
        private short eventZoneFlag;
        private short regionId;
    }
    
    /**
     * Navigation cell in sector.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavCellRefData {
        private float minX;
        private float minY;
        private float maxX;
        private float maxY;
        @Builder.Default
        private List<Short> navObjectIndices = new ArrayList<>();
        @Builder.Default
        private List<Short> navBorderIndices = new ArrayList<>();
        @Builder.Default
        private List<Short> navCellLinkIndices = new ArrayList<>();
    }
    
    /**
     * Border link between sectors.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavBorderRefData {
        private float minX;
        private float minY;
        private float maxX;
        private float maxY;
        private byte lineFlag;
        private byte lineSource;
        private byte lineDestination;
        private short cellSourceIndex;
        private short cellDestinationIndex;
        private short regionSourceYX;
        private short regionDestinationYX;
    }
    
    /**
     * Cell link within sector.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavCellLinkRefData {
        private float minX;
        private float minY;
        private float maxX;
        private float maxY;
        private byte lineFlag;
        private byte lineSource;
        private byte lineDestination;
        private short cellSourceIndex;
        private short cellDestinationIndex;
    }
}
