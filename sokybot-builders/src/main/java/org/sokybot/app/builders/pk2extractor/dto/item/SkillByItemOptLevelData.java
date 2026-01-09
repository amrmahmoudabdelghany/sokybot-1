package org.sokybot.app.builders.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for skill-item-opt link data.
 * Links a skill to an item optimization level.
 * Reference: RSBot RefSkillByItemOptLevel
 */
@Data
@Builder
public class SkillByItemOptLevelData {
    
    private int linkId;           // Link ID
    private int skillId;          // Skill ID
}
