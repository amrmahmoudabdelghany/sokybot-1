package org.sokybot.app.builders.pk2extractor.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for extra item optimization ability data.
 * Defines additional skills/bonuses granted by item enhancement levels.
 * Reference: RSBot RefExtraAbilityByEquipItemOptLevel
 */
@Data
@Builder
public class ExtraItemOptAbilityData {
    
    private int itemId;           // Item reference ID
    private byte optLevel;        // Enhancement level
    private List<Integer> skillIds; // List of granted skill IDs
}
