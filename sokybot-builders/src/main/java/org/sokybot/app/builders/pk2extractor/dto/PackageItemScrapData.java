package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for package item scrap data from PK2 files.
 * Defines the contents of a package item (what items are inside a mall box/bundle).
 * Reference: RSBot RefPackageItemScrap
 */
@Data
@Builder
public class PackageItemScrapData {
    
    private String packageItemCodeName;   // Parent package reference
    private String itemCodeName;          // Contained item reference
    private byte optLevel;                // Item enhancement level
    private long variance;                // Item variance
    private int data;                     // Generic data (durability/count)
    private int index;                    // Position index
    
    // Magic parameters (if needed)
    private long magicParam1;
    private long magicParam2;
}
