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
    
    // Identity
    private int refId;
    private String longId;
    private String name;
    
    // Core properties
    private boolean isMallItem;
    private ItemTypeData itemType;
    private RaceData race;
    private GenderData gender;
    private boolean isSOX;
    private int level;
    private int degree;
    private int maxStacks;
    private boolean isSortable;
    
    // Enhanced fields (Phase 10)
    private byte rarity;
    private int price;            // Buy price
    private int sellPrice;        // Sell to NPC price
    private String iconPath;
    
    // Flags
    private boolean canTrade;
    private boolean canSell;
    private boolean canBuy;
    private boolean canDrop;
    private boolean canRepair;
    private boolean canRevive;
    private boolean canUse;
}
