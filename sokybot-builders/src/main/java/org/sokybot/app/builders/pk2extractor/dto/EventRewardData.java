package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for event reward item data from PK2 files.
 * Event rewards define items given during special events.
 * Reference: RSBot RefEventRewardItems
 */
@Data
@Builder
public class EventRewardData {
    
    private int eventId;
    private String eventCodeName;     // Event identifier
    private String itemCodeName;      // Reward item reference
    private int itemAmount;           // Quantity
    private int minRequiredLevel;     // Min level to receive
    private int maxRequiredLevel;     // Max level to receive
}
