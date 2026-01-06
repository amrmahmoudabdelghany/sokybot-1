import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Item data extracted from pk2 files.
 * Plain POJO without JPA annotations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemData {
    
    private int refId;
    private String longId;
    private String name;
    
    private boolean isMallItem;
    private ItemTypeData itemType;
    private RaceData race;
    private GenderData gender;
    private boolean isSOX;
    private int level;
    private int degree;
    private int maxStacks;
    private boolean isSortable;
}




