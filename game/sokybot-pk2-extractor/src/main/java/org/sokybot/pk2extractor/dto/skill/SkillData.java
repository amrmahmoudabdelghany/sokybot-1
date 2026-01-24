package org.sokybot.pk2extractor.dto.skill;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Skill data extracted from pk2 files.
 * Enhanced with skill parameters from skrillax skilldata.rs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillData {
    private int refId;
    private String longId;
    private String name;
    
    // Time & Cooldowns
    private int castTime;
    private int cooldown;
    private int duration;
    private int preparingTime;
    
    // Costs
    private int MP;
    private int consumeHP;
    
    // Requirements
    private int masteryId;
    private int reqCommonMastery1;
    private int reqCommonMasteryLevel1;
    private byte reqCastWeapon1;
    private byte reqCastWeapon2;
    
    // Attributes
    private boolean targetRequired;
    private String iconPath;
    private String type;
    
    // Enhanced fields from skrillax
    
    /** Skill effects/parameters (up to 10 per skill) */
    private List<SkillEffect> effects;
    
    /** Target type (self, ally, enemy, ground, etc.) */
    private SkillTargetType targetType;
    
    /** Weapon type requirements (0 = any) */
    private int[] weaponRequirements;
    
    /** Skill range in game units */
    private int range;
    
    /** Attack/effect distance */
    private int attackDistance;
    
    /** AOE radius (0 for single target) */
    private int aoeRange;
    
    /** Whether skill can be used while moving */
    private boolean canCastWhileMoving;
    
    /** Max targets for AOE skills */
    private int maxTargets;
    
    /** Skill level (within mastery) */
    private int skillLevel;
}

