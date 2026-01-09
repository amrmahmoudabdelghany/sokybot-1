package org.sokybot.pk2extractor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for item type data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemTypeData {
    private int type1;
    private int type2;
    private int type3;
    private int type4;
    
    public static ItemTypeData of(int value) {
        return new ItemTypeData(value, 0, 0, 0);
    }
    
    public static ItemTypeData of(int type1, int type2, int type3, int type4) {
        return new ItemTypeData(type1, type2, type3, type4);
    }
    
    public int getValue() {
        return type1;
    }
}
