package org.sokybot.app.builders.pk2extractor.dto;

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
    private int preparingTime;    // Action_PreparingTime
    
    // Costs
    private int MP;
    private int consumeHP;        // Consume_HP
    
    // Requirements
    private int masteryId;        // Param 34
    private int reqCommonMastery1; // ReqCommon_Mastery1
    private int reqCommonMasteryLevel1; // ReqCommon_MasteryLevel1
    private byte reqCastWeapon1;   // ReqCast_Weapon1
    private byte reqCastWeapon2;   // ReqCast_Weapon2
    
    // Attributes
    private boolean targetRequired;
    private String iconPath;       // UI_IconFile
    private String type;           // Skill type description or enum string
}
