package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for shop group mapping data from PK2 files.
 * Maps specific shops to shop groups.
 * Reference: RSBot RefMappingShopGroup
 */
@Data
@Builder
public class RefMappingShopGroupData {
    
    private String groupCodeName;     // Group identifier
    private String shopCodeName;      // Shop identifier
    private int country;              // Country filter
}
