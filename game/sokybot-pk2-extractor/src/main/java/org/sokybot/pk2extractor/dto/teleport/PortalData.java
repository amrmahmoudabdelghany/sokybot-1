package org.sokybot.pk2extractor.dto.teleport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Portal data extracted from pk2 files.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalData {
    private int refId;
    private String longId;
    private String name;
}
