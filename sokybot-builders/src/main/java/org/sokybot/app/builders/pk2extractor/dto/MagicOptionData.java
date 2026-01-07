package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for magic option (blue stats) data from PK2 files.
 * Magic options are item stat bonuses that can be added through alchemy.
 */
@Data
@Builder
public class MagicOptionData {
    
    private int id;
    private String longId;
    private String name;
    private int attributeType;    // Type of attribute (PA, MA, HP, etc.)
    private int minValue;
    private int maxValue;
    private int degree;           // Required item degree
    private boolean isWeapon;     // Applicable to weapons
    private boolean isArmor;      // Applicable to armor
    private boolean isAccessory;  // Applicable to accessories
    private boolean isShield;     // Applicable to shields
    
    /**
     * Attribute type constants.
     */
    public interface AttributeType {
        int PHYSICAL_ATTACK = 1;
        int MAGICAL_ATTACK = 2;
        int PHYSICAL_DEFENSE = 3;
        int MAGICAL_DEFENSE = 4;
        int HIT_RATE = 5;
        int EVASION_RATE = 6;
        int PARRY_RATE = 7;
        int CRITICAL_RATE = 8;
        int HP = 9;
        int MP = 10;
        int STR = 11;
        int INT = 12;
    }
}
