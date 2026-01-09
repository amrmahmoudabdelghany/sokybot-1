package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for package item data from PK2 files.
 * Package items are bundled items (like item mall boxes that contain multiple items).
 * Reference: RSBot RefPackageItem
 */
@Data
@Builder
public class PackageItemData {
    
    private int id;
    private String packageCodeName;   // Package reference name
    private String itemCodeName;      // Contained item reference name
    private byte optLevel;            // Enhancement level
    private long variance;            // Item variance/attributes
    private int durability;           // Item durability
    private int quantity;             // Stack count
}
