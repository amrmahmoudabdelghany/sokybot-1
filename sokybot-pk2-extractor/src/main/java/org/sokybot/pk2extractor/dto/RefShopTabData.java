package org.sokybot.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for Shop Tab definition.
 */
@Data
@Builder
public class RefShopTabData {
    
    private int id;
    private String codeName;
    private String refTabGroupCodeName;
    private String strID128_Tab;
    private int country;
    private int service;
}
