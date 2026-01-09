package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for shop good data from PK2 files.
 * Shop goods define items available in NPC shops.
 * Reference: RSBot RefShopGood
 */
@Data
@Builder
public class ShopGoodData {
    
    private int id;
    private String tabCodeName;           // Shop tab reference
    private String packageItemCodeName;   // Item package reference
    private byte slotIndex;               // Position in shop tab
    private int country;                  // Country filter
}
