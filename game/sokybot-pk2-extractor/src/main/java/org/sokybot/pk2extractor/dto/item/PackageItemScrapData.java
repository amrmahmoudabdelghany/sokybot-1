package org.sokybot.pk2extractor.dto.item;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for package item scrap data from PK2 files.
 */
@Data
@Builder
public class PackageItemScrapData {
    
    private String packageItemCodeName;
    private String itemCodeName;
    private byte optLevel;
    private long variance;
    private int data;
    private int index;
    private long magicParam1;
    private long magicParam2;
}
