package org.sokybot.pk2extractor.dto.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for enhanced Character/NPC data extracted from pk2 files.
 * Provides more details than legacy NPCData (MP, Gender, Vehicle flags, etc.).
 * Reference: RSBot RefObjChar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterData {
    
    // Identity
    private int refId;
    private String longId;
    private String name;
    
    // Stats
    private int level;
    private int maxHP;
    private int maxMP;
    
    // Attributes
    private byte rarity; // MonsterRarity
    private GenderData gender;
    
    // Inventory & Storage (Pet/Vehicle)
    private int inventorySize;
    private boolean canStoreTID1;
    private boolean canStoreTID2;
    private boolean canStoreTID3;
    private boolean canStoreTID4;
    
    // Vehicle/Pet Flags
    private boolean canBeVehicle;
    private boolean canControl;
    private int maxPassenger;
    
    // Flags from RefObjCommon
    private boolean isDimensionPillar;
    private boolean isSummonFlower;
    private boolean isEventMob;
    
    // Movement (from skrillax characterdata.rs)
    private int walkSpeed;      // column 46
    private int runSpeed;       // column 47
    private int berserkSpeed;   // column 48
    
    // Combat
    private int baseRange;      // column 50
    private int pickupRange;    // column 61
    private boolean aggressive; // column 93
    
    // Progression
    private int exp;            // column 79
    
    // Skills (columns 83-92, non-zero skill IDs)
    private int[] skillIds;
}
