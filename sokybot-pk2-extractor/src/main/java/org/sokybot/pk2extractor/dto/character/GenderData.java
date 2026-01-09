package org.sokybot.pk2extractor.dto.character;

import lombok.Getter;

/**
 * DTO for gender data.
 */
@Getter
public enum GenderData {
    MALE(1),
    FEMALE(2);
    
    private final int value;
    
    GenderData(int value) {
        this.value = value;
    }
    
    public static GenderData of(int value) {
        for (GenderData gender : values()) {
            if (gender.value == value) {
                return gender;
            }
        }
        return null;
    }
}
