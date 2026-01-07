package org.sokybot.pk2extractor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Shop data extracted from pk2 files.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopData {
    private int refId;
    private String longId;
    private String name;
}
