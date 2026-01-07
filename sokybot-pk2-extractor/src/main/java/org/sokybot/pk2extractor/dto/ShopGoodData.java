package org.sokybot.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for shop good data from PK2 files.
 */
@Data
@Builder
public class ShopGoodData {
    
    private int id;
    private String tabCodeName;
    private String packageItemCodeName;
    private byte slotIndex;
    private int country;
}
