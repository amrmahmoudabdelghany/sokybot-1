package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for teleport link data from PK2 files.
 * Teleport links define connections between teleport locations and their fees.
 * Reference: RSBot RefTeleportLink
 */
@Data
@Builder
public class TeleportLinkData {
    
    private int id;
    private int ownerTeleportId;     // Source teleport location
    private int targetTeleportId;    // Destination teleport location
    private int fee;                 // Gold cost
    private byte restrictBindMethod;
    private byte checkResult;
    
    // Restriction data (level, job, etc.)
    private int restrict1;
    private int data1_1;
    private int data1_2;
    
    private int restrict2;
    private int data2_1;
    private int data2_2;
}
