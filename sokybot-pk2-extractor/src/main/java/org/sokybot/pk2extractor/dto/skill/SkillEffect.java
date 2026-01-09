package org.sokybot.pk2extractor.dto.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single skill effect/parameter.
 * Skills can have multiple effects defined by paramType/value pairs.
 * Reference: skrillax skilldata.rs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillEffect {
    
    /** Parameter type code (maps to SkillParamType) */
    private int paramType;
    
    /** Primary value for this effect (damage amount, buff %, duration, etc.) */
    private int value;
    
    /** Secondary value (used for ranges, scaling factors, etc.) */
    private int value2;
    
    /** Probability of effect triggering (0-100) */
    private int probability;
    
    /** Duration in milliseconds (0 for instant effects) */
    private int duration;
    
    /**
     * Get the typed parameter type.
     */
    public SkillParamType getType() {
        return SkillParamType.fromCode(paramType);
    }
    
    /**
     * Check if this effect is a debuff.
     */
    public boolean isDebuff() {
        return getType().isDebuff();
    }
    
    /**
     * Check if this effect is a buff.
     */
    public boolean isBuff() {
        return getType().isBuff();
    }
    
    /**
     * Check if this effect deals damage.
     */
    public boolean isDamage() {
        return getType().isDamage();
    }
}
