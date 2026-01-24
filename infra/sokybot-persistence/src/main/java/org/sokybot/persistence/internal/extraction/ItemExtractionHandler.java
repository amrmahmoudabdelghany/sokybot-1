package org.sokybot.persistence.internal.extraction;

import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.entities.ItemType;
import org.sokybot.persistence.entities.Gender;
import org.sokybot.persistence.entities.Race;
import org.sokybot.pk2extractor.dto.item.ItemData;
import org.sokybot.pk2extractor.mediapk2.item.ItemDataExtractor;

import javax.persistence.EntityManagerFactory;

/**
 * Item extraction handler.
 */
public class ItemExtractionHandler extends BasePk2ExtractionHandler<ItemData, ItemEntity> {
    
    public ItemExtractionHandler(EntityManagerFactory emf) {
        super(emf, new ItemDataExtractor());
    }
    
    @Override
    protected ItemEntity convertDtoToEntity(ItemData dto) {
        return ItemEntity.builder()
                .refId(dto.getRefId())
                .longId(dto.getLongId())
                .name(dto.getName())
                .isMallItem(dto.isMallItem())
                .iconPath(dto.getIconPath())
                .level(dto.getLevel())
                .degree(dto.getDegree())
                .maxStacks(dto.getMaxStacks())
                .isSortable(dto.isSortable())
                .isSOX(dto.isSOX())
                .itemType(dto.getItemType() != null ? 
                    ItemType.parseType(dto.getItemType().getValue(), dto.getLongId()) : null)
                .race(dto.getRace() != null ? 
                    Race.parseType((byte)dto.getRace().getValue()) : null)
                .gender(dto.getGender() != null ? 
                    Gender.parseType((byte)dto.getGender().getValue()) : null)
                .build();
    }
    
    @Override
    public String getExtractionType() {
        return "Item";
    }
}