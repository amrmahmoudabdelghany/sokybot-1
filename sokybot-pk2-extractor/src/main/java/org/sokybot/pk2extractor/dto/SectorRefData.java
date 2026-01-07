package org.sokybot.pk2extractor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for SectorRef data extracted from pk2 files.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorRefData {
    private short sectorId;
}
