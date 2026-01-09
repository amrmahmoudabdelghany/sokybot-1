package org.sokybot.pk2extractor.dto.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for NPC data extracted from pk2 files.
 * Plain POJO without JPA annotations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NPCData {
    
    private int refId;
    private String longId;
    private String name;
    private int level;
    private int HP;
    private NPCTypeData type;
    
    public boolean isMonster() {
        return this.longId != null && this.longId.contains("MOB_");
    }
    
    public boolean isCharacter() {
        return this.longId != null && this.longId.contains("CHAR_");
    }
}
