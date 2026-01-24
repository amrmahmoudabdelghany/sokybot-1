package org.sokybot.pk2extractor.dto.item.magic;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for magic option assignment data from PK2 files.
 */
@Data
@Builder
public class MagicOptionAssignmentData {
    
    private byte race;
    private byte typeId3;
    private byte typeId4;
    private List<String> availableMagicOptions;
}
