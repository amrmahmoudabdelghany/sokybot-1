package org.sokybot.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for full level data from PK2 files.
 */
@Data
@Builder
public class FullLevelData {
    
    private int level;
    private long playerExp;
    private int masteryExp;
    private long petExp;
    private int petStoredSp;
    private int jobTraderExp;
    private int jobRobberExp;
    private int jobHunterExp;
}
