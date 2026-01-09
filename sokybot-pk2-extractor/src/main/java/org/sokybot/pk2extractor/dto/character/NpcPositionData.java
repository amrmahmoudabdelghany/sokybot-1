package org.sokybot.pk2extractor.dto.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for NPC spawn position data extracted from NpcPos.txt.
 * Contains world coordinates where NPCs are spawned.
 * Reference: skrillax npc_pos.rs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NpcPositionData {
    
    /** NPC reference ID */
    private int npcId;
    
    /** Region ID where NPC spawns */
    private int region;
    
    /** X coordinate in world space */
    private float x;
    
    /** Y coordinate (height) in world space */
    private float y;
    
    /** Z coordinate in world space */
    private float z;
}
