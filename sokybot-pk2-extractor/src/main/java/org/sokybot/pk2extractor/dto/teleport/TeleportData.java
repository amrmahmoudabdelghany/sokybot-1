package org.sokybot.pk2extractor.dto.teleport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for Teleport data extracted from pk2 files.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeleportData {
    private int refId;
    private String longId;
    private String name;
    private int portalId;
    @Builder.Default
    private List<Integer> links = new ArrayList<>();
}
