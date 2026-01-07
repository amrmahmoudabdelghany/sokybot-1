package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for region/zone data from PK2 files.
 */
@Data
@Builder
public class RegionData {
    
    private int id;
    private String longId;
    private String name;
    private int type;           // Region type
    private int areaId;         // Parent area
    private boolean isPvP;      // PvP enabled
    private boolean isSafe;     // Safe zone (town)
    private int minLevel;
    private int maxLevel;
    
    /**
     * Region type constants.
     */
    public interface RegionType {
        int FIELD = 1;
        int TOWN = 2;
        int DUNGEON = 3;
        int JOB_TEMPLE = 4;
        int FORTRESS = 5;
        int BATTLE_ARENA = 6;
    }
}
