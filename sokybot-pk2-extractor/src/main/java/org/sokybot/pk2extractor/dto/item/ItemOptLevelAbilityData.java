package org.sokybot.pk2extractor.dto.item;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for item optimization level ability data from PK2 files.
 */
@Data
@Builder
public class ItemOptLevelAbilityData {
    
    private int id;
    private int itemId;
    private byte optLevel;
    private int skillId;
    private int bonusValue;
    
    public interface OptLevel {
        byte PLUS_1 = 1;
        byte PLUS_2 = 2;
        byte PLUS_3 = 3;
        byte PLUS_4 = 4;
        byte PLUS_5 = 5;
        byte PLUS_6 = 6;
        byte PLUS_7 = 7;
        byte PLUS_8 = 8;
        byte PLUS_9 = 9;
        byte PLUS_10 = 10;
        byte PLUS_11 = 11;
        byte PLUS_12 = 12;
        byte ADV_PLUS_1 = 13;
        byte ADV_PLUS_2 = 14;
    }
}
