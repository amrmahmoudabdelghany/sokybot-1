package org.sokybot.pk2extractor.dto.shop;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for shop group mapping data from PK2 files.
 */
@Data
@Builder
public class RefMappingShopGroupData {
    
    private String groupCodeName;
    private String shopCodeName;
    private int country;
}
