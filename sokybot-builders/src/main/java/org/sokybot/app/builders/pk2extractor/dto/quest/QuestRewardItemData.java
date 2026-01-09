package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for quest item reward data from PK2 files.
 * Defines specific item rewards for quests.
 * Reference: RSBot RefQuestRewardItem
 */
@Data
@Builder
public class QuestRewardItemData {
    
    private int questId;
    private String questCodeName;
    private byte rewardType;
    private String itemCodeName;          // Main reward item
    private String optionalItemCode;      // Optional item choice
    private int optionalItemCount;
    private int achieveQuantity;          // Required quantity?
    private String rentItemCodeName;      // Rental item reward
}
