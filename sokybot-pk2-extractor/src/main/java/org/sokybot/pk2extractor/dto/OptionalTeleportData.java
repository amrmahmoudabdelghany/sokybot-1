package org.sokybot.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for optional teleport data from PK2 files.
 */
@Data
@Builder
public class OptionalTeleportData {
    
    private int id;
    private String objName128;
    private String zoneName128;
    private int regionId;
    private float posX;
    private float posY;
    private float posZ;
    private int worldId;
    private int minLevel;
    private int maxLevel;
}
