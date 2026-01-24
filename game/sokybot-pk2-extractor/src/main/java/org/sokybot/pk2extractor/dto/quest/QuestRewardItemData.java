package org.sokybot.pk2extractor.dto.quest;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for quest item reward data from PK2 files.
 */
@Data
@Builder
public class QuestRewardItemData {
    
    private int questId;
    private String questCodeName;
    private byte rewardType;
    private String itemCodeName;
    private String optionalItemCode;
    private int optionalItemCount;
    private int achieveQuantity;
    private String rentItemCodeName;
}
