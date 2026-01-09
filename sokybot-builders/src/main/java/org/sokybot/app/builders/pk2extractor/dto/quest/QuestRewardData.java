package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for detailed quest reward data from PK2 files.
 * Defines complex rewards for completion including Gold, SP, Items, etc.
 * Reference: RSBot RefQuestReward
 */
@Data
@Builder
public class QuestRewardData {
    
    private int questId;
    private String questCodeName;
    private boolean isView;
    private boolean isBasicReward;
    private boolean isItemReward;
    
    // Check flags
    private boolean isCheckCondition;
    private boolean isCheckCountry;
    private boolean isCheckClass;
    private boolean isCheckGender;
    
    // Rewards
    private int gold;
    private int exp;     // Experience
    private int spExp;   // Skill Experience
    private int sp;      // Skill Points
    private int ap;      // Arena Points?
    private String apType;
    private byte hwan;   // Hwan/Zerk level?
    private byte inventorySlots; // Expand inventory
    
    // Item Reward Logic
    private byte itemRewardType;
    private byte selectionCount;
}
