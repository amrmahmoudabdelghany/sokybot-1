package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for localization text data from PK2 files.
 * Maps string identifiers (SN_*) to localized text.
 * Reference: RSBot RefText
 */
@Data
@Builder
public class RefTextData {
    
    private String nameStrId;     // Key (e.g., SN_ITEM_01)
    private String data;          // Localized string content
    private int service;          // Service flag
}
