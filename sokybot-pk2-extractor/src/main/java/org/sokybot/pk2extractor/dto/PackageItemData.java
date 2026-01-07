package org.sokybot.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for package item data from PK2 files.
 */
@Data
@Builder
public class PackageItemData {
    
    private int id;
    private String packageCodeName;
    private String itemCodeName;
    private byte optLevel;
    private long variance;
    private int durability;
    private int quantity;
}
