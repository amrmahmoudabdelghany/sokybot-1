package org.sokybot.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for shop group data from PK2 files.
 */
@Data
@Builder
public class ShopGroupData {
    
    private int id;
    private String codeName;
    private String refNpcCodeName;
    private int country;
}
