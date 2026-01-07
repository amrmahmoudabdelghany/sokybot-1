package org.sokybot.app.builders.pk2extractor.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for magic option assignment data from PK2 files.
 * Defines which magic options (blue stats) can be applied to specific item types.
 * Reference: RSBot RefMagicOptAssign
 */
@Data
@Builder
public class MagicOptionAssignmentData {
    
    private byte race;            // Item race (0: CH, 1: EU)
    private byte typeId3;         // Item TypeID3 (Weapon/Armor/etc category)
    private byte typeId4;         // Item TypeID4 (Sub-category)
    private List<String> availableMagicOptions; // List of MagicOption code names
}
