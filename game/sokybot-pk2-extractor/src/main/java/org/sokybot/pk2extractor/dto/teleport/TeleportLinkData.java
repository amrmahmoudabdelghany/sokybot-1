package org.sokybot.pk2extractor.dto.teleport;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for teleport link data from PK2 files.
 */
@Data
@Builder
public class TeleportLinkData {
    
    private int id;
    private int ownerTeleportId;
    private int targetTeleportId;
    private int fee;
    private byte restrictBindMethod;
    private byte checkResult;
    private int restrict1;
    private int data1_1;
    private int data1_2;
    private int restrict2;
    private int data2_1;
    private int data2_2;
}
