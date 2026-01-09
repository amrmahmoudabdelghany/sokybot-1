package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for optional teleport data from PK2 files.
 * Defines special or dynamic teleport points (e.g., event zones, reverse return points).
 * Reference: RSBot RefOptionalTeleport
 */
@Data
@Builder
public class OptionalTeleportData {
    
    private int id;
    private String objName128;        // Object name reference
    private String zoneName128;       // Zone name reference
    private int regionId;             // Region ID
    private float posX;
    private float posY;
    private float posZ;
    private int worldId;
    private int minLevel;
    private int maxLevel;
}
