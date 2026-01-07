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
    private int value;
    
    public static ItemTypeData of(int value) {
        return new ItemTypeData(value);
    }
}
