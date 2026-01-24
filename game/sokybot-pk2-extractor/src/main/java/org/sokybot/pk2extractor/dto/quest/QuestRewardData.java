package org.sokybot.pk2extractor.dto.quest;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for detailed quest reward data from PK2 files.
 */
@Data
@Builder
public class QuestRewardData {
    
    private int questId;
    private String questCodeName;
    private boolean isView;
    private boolean isBasicReward;
    private boolean isItemReward;
    private boolean isCheckCondition;
    private boolean isCheckCountry;
    private boolean isCheckClass;
    private boolean isCheckGender;
    private int gold;
    private int exp;
    private int spExp;
    private int sp;
    private int ap;
    private String apType;
    private byte hwan;
    private byte inventorySlots;
    private byte itemRewardType;
    private byte selectionCount;
}
