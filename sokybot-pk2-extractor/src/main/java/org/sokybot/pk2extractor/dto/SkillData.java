package org.sokybot.pk2extractor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Skill data extracted from pk2 files.
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
}
