package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for skill mastery data from PK2 files.
 * Skill masteries define skill trees and weapon specializations.
 * Reference: RSBot RefSkillMastery
 */
@Data
@Builder
public class SkillMasteryData {
    
    private int id;
    private String nameCode;          // Display name reference
    private String descriptionCode;   // Description reference
    private byte groupNum;            // Mastery group
    private byte tabId;               // UI tab position
    private byte weaponType1;         // Primary weapon type
    private byte weaponType2;         // Secondary weapon type
    private byte weaponType3;         // Tertiary weapon type
    private String iconPath;          // Mastery icon
    
    /**
     * Weapon type constants.
     */
    public interface WeaponType {
        byte NONE = 0;
        byte SWORD = 1;
        byte BLADE = 2;
        byte SPEAR = 3;
        byte GLAIVE = 4;
        byte BOW = 5;
        byte ONE_HAND_SWORD = 6;
        byte TWO_HAND_SWORD = 7;
        byte AXE = 8;
        byte WARLOCK = 9;
        byte STAFF = 10;
        byte CROSSBOW = 11;
        byte DAGGER = 12;
        byte HARP = 13;
        byte CLERIC = 14;
    }
}
