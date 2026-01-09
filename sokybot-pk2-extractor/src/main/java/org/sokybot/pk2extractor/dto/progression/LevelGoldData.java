package org.sokybot.pk2extractor.dto.progression;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for level-based gold drop data extracted from levelgold.txt.
 * Defines the gold drop range for monsters at each level.
 * Reference: skrillax gold.rs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelGoldData {
    
    /** Monster/Entity level */
    private int level;
    
    /** Minimum gold drop amount */
    private int minGold;
    
    /** Maximum gold drop amount */
    private int maxGold;
}
