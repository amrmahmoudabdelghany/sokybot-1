package org.sokybot.pk2extractor.dto.quest;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for event reward item data from PK2 files.
 */
@Data
@Builder
public class EventRewardData {
    
    private int eventId;
    private String eventCodeName;
    private String itemCodeName;
    private int itemAmount;
    private int minRequiredLevel;
    private int maxRequiredLevel;
}
