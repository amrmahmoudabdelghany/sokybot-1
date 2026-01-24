package org.sokybot.pk2extractor.dto.skill;

/**
 * Enum representing skill parameter types.
 * Based on skrillax skilldata.rs SkillParam enum.
 * Each type defines what effect a skill parameter produces.
 */
public enum SkillParamType {
    
    // Attack types (1-10)
    ATTACK(1),
    ATTACK_PIERCE(2),
    
    // Debuffs - Status effects (11-30)
    FEAR(6),
    BLEED(7),
    DULL(8),
    BLIND(9),
    DISEASE(10),
    DIVISION(11),
    POISON(12),
    WEAKEN(13),
    ZOMBIE(14),
    SLEEP(15),
    BURN(16),
    FROSTBITE(17),
    ELECTRIC_SHOCK(18),
    STUN(19),
    ROOT(20),
    FREEZE(21),
    
    // Control effects (31-40)
    KNOCKBACK(25),
    KNOCKDOWN(26),
    TAUNT(27),
    PUSH(28),
    PULL(29),
    
    // Buff - Heal/Restore (41-50)
    HEAL_HP(30),
    HEAL_MP(31),
    HEAL_HP_PCT(32),
    HEAL_MP_PCT(33),
    HEAL_HP_MP(34),
    HEAL_STATUS(35),
    
    // Buff - Stat modifiers (51-70)
    BUFF_ATTACK(40),
    BUFF_DEFENSE(41),
    BUFF_HP(42),
    BUFF_MP(43),
    BUFF_HIT_RATE(44),
    BUFF_PARRY_RATE(45),
    BUFF_CRIT_RATE(46),
    BUFF_BLOCK_RATE(47),
    BUFF_SPEED(48),
    BUFF_ATTACK_SPEED(49),
    BUFF_CAST_SPEED(50),
    
    // Damage modifiers
    DAMAGE_ABSORB(60),
    DAMAGE_REFLECT(61),
    DAMAGE_REDUCTION(62),
    DAMAGE_AMPLIFY(63),
    
    // Special effects
    TELEPORT(70),
    STEALTH(71),
    DETECT_STEALTH(72),
    RESURRECT(73),
    SUMMON(74),
    CAPTURE(75),
    TRANSFORM(76),
    
    // Passive effects
    PASSIVE_ATTACK(80),
    PASSIVE_DEFENSE(81),
    PASSIVE_HP(82),
    PASSIVE_MP(83),
    PASSIVE_RANGE(84),
    
    // AOE flags
    AOE(90),
    AOE_CONE(91),
    AOE_LINE(92),
    
    // Unknown/Other
    UNKNOWN(0);
    
    private final int code;
    
    SkillParamType(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
    /**
     * Get SkillParamType from code.
     * Maps common known codes to types.
     */
    public static SkillParamType fromCode(int code) {
        // Common mappings from skrillax
        switch (code) {
            case 1: return ATTACK;
            case 2: return ATTACK_PIERCE;
            case 6: return FEAR;
            case 7: return BLEED;
            case 8: return DULL;
            case 9: return BLIND;
            case 10: return DISEASE;
            case 11: return DIVISION;
            case 12: return POISON;
            case 13: return WEAKEN;
            case 14: return ZOMBIE;
            case 15: return SLEEP;
            case 16: return BURN;
            case 17: return FROSTBITE;
            case 18: return ELECTRIC_SHOCK;
            case 19: return STUN;
            case 20: return ROOT;
            case 21: return FREEZE;
            case 25: return KNOCKBACK;
            case 26: return KNOCKDOWN;
            case 27: return TAUNT;
            case 28: return PUSH;
            case 29: return PULL;
            case 30: return HEAL_HP;
            case 31: return HEAL_MP;
            case 32: return HEAL_HP_PCT;
            case 33: return HEAL_MP_PCT;
            case 34: return HEAL_HP_MP;
            case 35: return HEAL_STATUS;
            default: return UNKNOWN;
        }
    }
    
    /**
     * Check if this param type is a debuff.
     */
    public boolean isDebuff() {
        return code >= 6 && code <= 29;
    }
    
    /**
     * Check if this param type is a buff.
     */
    public boolean isBuff() {
        return code >= 30 && code <= 70;
    }
    
    /**
     * Check if this param type deals damage.
     */
    public boolean isDamage() {
        return code >= 1 && code <= 5;
    }
    
    /**
     * Check if this param type is a passive effect.
     */
    public boolean isPassive() {
        return code >= 80 && code <= 89;
    }
}
