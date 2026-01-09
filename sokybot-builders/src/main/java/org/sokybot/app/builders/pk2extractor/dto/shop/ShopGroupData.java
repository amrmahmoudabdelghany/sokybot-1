package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for shop group data from PK2 files.
 * Defines groups of shops (often linked to an NPC).
 * Reference: RSBot RefShopGroup
 */
@Data
@Builder
public class ShopGroupData {
    
    private int id;
    private String codeName;          // Group identifier
    private String refNpcCodeName;    // Associated NPC
    private int country;              // Country filter
}
