package org.sokybot.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for shop with tab mapping data from PK2 files.
 */
@Data
@Builder
public class RefMappingShopWithTabData {
    
    private String shopCodeName;
    private String tabCodeName;
    private int country;
}
