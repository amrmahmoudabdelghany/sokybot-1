package org.sokybot.pk2extractor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for NPC type data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NPCTypeData {
    private int value;
    
    public static NPCTypeData of(int value) {
        return new NPCTypeData(value);
    }
}
