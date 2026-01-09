package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for shop with tab mapping data from PK2 files.
 * Maps specific tabs to shops.
 * Reference: RSBot RefMappingShopWithTab
 */
@Data
@Builder
public class RefMappingShopWithTabData {
    
    private String shopCodeName;      // Shop identifier
    private String tabCodeName;       // Tab identifier
    private int country;              // Country filter
}
