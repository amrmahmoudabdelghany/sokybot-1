package org.sokybot.pk2extractor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for skill-item-opt link data.
 */
@Data
@Builder
public class SkillByItemOptLevelData {
    
    private int linkId;
    private int skillId;
}
