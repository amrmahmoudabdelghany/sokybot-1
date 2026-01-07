package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for full level data from PK2 files.
 * Enhanced version of LvlData to include mastery, pet, and job exp requirements.
 * Reference: RSBot RefLevel
 */
@Data
@Builder
public class FullLevelData {
    
    private int level;
    private long playerExp;       // Exp_C
    private int masteryExp;       // Exp_M
    private long petExp;          // Exp_C_Pet2 (Newer files)
    private int petStoredSp;      // StoredSp_Pet2
    
    // Job Exp (Commented in RSBot but often present)
    private int jobTraderExp;
    private int jobRobberExp;
    private int jobHunterExp;
}
