package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for Shop Tab definition.
 * Reference: RSBot RefShopTab
 */
@Data
@Builder
public class RefShopTabData {
    
    private int id;
    private String codeName;          // Tab identifier (e.g., STORE_CONST_TAB1)
    private String refTabGroupCodeName; 
    private String strID128_Tab;      // Localized name key
    private int country;
    private int service;
}
