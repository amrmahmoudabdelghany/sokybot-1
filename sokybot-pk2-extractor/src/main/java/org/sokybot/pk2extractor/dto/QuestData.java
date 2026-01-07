package org.sokybot.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for quest data extracted from PK2 files.
 */
@Data
@Builder
public class QuestData {
    
    private int id;
    private String longId;
    private String name;
    private int type;
    private int minLevel;
    private int maxLevel;
    private int repeatCount;
    private boolean isParty;
    private int npcRefId;
    private int rewardExp;
    private int rewardGold;
    private int rewardSkillPoint;
    
    /**
     * Quest type constants.
     */
    public interface QuestType {
        int STORY = 1;
        int JOB = 2;
        int PARTY = 3;
        int DAILY = 4;
        int WEEKLY = 5;
        int TUTORIAL = 6;
    }
}
