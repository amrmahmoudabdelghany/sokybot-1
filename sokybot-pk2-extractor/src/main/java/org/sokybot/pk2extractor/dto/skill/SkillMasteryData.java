package org.sokybot.pk2extractor.dto.skill;

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
    private String nameCode;
    private String descriptionCode;
    private byte groupNum;
    private byte tabId;
    private byte weaponType1;
    private byte weaponType2;
    private byte weaponType3;
    private String iconPath;
    
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
