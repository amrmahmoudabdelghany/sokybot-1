package org.sokybot.pk2extractor.dto.skill;

/**
 * Enum representing skill targeting options.
 * Based on skrillax skilldata.rs target options.
 */
public enum SkillTargetType {
    
    NONE(0),
    SELF(1),
    ALLY(2),
    PARTY(3),
    GUILD(4),
    ENEMY_PLAYER(5),
    ENEMY_MONSTER(6),
    ANY_ENTITY(7),
    GROUND(8),
    CORPSE(9),
    PET(10),
    TRANSPORT(11);
    
    private final int code;
    
    SkillTargetType(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
    public static SkillTargetType fromCode(int code) {
        for (SkillTargetType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return NONE;
    }
    
    /**
     * Check if this target type is hostile.
     */
    public boolean isHostile() {
        return this == ENEMY_PLAYER || this == ENEMY_MONSTER;
    }
    
    /**
     * Check if this target type is friendly.
     */
    public boolean isFriendly() {
        return this == SELF || this == ALLY || this == PARTY || this == GUILD || this == PET;
    }
}
