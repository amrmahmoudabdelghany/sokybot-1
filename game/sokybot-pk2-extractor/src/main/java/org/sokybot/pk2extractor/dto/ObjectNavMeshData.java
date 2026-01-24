package org.sokybot.pk2extractor.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ObjectNavMesh data extracted from binary .bms files.
 * Contains navigation mesh data for game objects.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectNavMeshData {
    
    private int id;
    
    @Builder.Default
    private List<PositionData> points = new ArrayList<>();
    
    @Builder.Default
    private List<ObjectLineRefData> inLines = new ArrayList<>();
    
    @Builder.Default
    private List<ObjectLineRefData> outLines = new ArrayList<>();
    
    @Builder.Default
    private List<ObjectGroundTriData> objectGround = new ArrayList<>();
    
    private boolean outCanBlock;
    private boolean inCanBlock;
    private boolean hasEntrance;
    
    /**
     * Position data for navmesh vertices.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionData {
        private float x;
        private float z;
        private float y;
    }
    
    /**
     * Line reference for outline/inline edges.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObjectLineRefData {
        private short pointAIndex;
        private short pointBIndex;
        private int neighbourAIndex;
        private int neighbourBIndex;
        private byte flag;
    }
    
    /**
     * Triangle reference for ground cells.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObjectGroundTriData {
        private short pointA;
        private short pointB;
        private short pointC;
        private short unk;
    }
}
