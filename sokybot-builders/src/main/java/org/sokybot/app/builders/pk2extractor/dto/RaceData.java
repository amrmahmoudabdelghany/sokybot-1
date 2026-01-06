import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Getter;

/**
 * DTO for race data.
 */
@Getter
public enum RaceData {
    CHINESE(1),
    EUROPEAN(2);
    
    private final int value;
    
    RaceData(int value) {
        this.value = value;
    }
    
    public static RaceData of(int value) {
        for (RaceData race : values()) {
            if (race.value == value) {
                return race;
            }
        }
        return null;
    }
}




