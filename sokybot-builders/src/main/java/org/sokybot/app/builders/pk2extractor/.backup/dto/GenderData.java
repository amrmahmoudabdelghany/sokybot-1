import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.dto;

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




