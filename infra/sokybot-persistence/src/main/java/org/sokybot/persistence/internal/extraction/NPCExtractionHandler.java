package org.sokybot.persistence.internal.extraction;

import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.entities.NPCType;
import org.sokybot.pk2extractor.dto.character.NPCData;
import org.sokybot.pk2extractor.mediapk2.character.NPCDataExtractor;

import javax.persistence.EntityManagerFactory;

/**
 * NPC extraction handler.
 */
public class NPCExtractionHandler extends BasePk2ExtractionHandler<NPCData, NPCEntity> {
    
    public NPCExtractionHandler(EntityManagerFactory emf) {
        super(emf, new NPCDataExtractor());
    }
    
    @Override
    protected NPCEntity convertDtoToEntity(NPCData dto) {
        return NPCEntity.builder()
                .refId(dto.getRefId())
                .longId(dto.getLongId())
                .name(dto.getName())
                .level(dto.getLevel())
                .HP(dto.getHP())
                .Type(dto.getType() != null ? NPCType.of(dto.getType().getValue()) : null)
                .iconPath(dto.getIconPath())
                .build();
    }
    
    @Override
    public String getExtractionType() {
        return "NPC";
    }
}