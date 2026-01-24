package org.sokybot.pk2extractor.dto.item;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for extra item optimization ability data.
 */
@Data
@Builder
public class ExtraItemOptAbilityData {
    
    private int itemId;
    private byte optLevel;
    private List<Integer> skillIds;
}
