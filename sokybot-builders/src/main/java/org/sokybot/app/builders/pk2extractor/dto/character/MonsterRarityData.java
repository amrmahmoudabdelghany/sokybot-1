package org.sokybot.app.builders.pk2extractor.dto;

/**
 * Monster rarity classification enum from RSBot reference.
 * Used to classify monsters by their special status.
 */
public enum MonsterRarityData {
    
    NORMAL(0, "Normal"),
    CHAMPION(1, "Champion"),     // Blue name
    UNIQUE(2, "Unique"),         // Gold name
    GIANT(3, "Giant"),           // Large event monster
    TITAN(4, "Titan"),           // Rare spawn
    ELITE(5, "Elite"),           // Fortress/Job enemy
    EVENT(6, "Event");           // Event-only spawn
    
    private final int code;
    private final String displayName;
    
    MonsterRarityData(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
    
    public int getCode() { return code; }
    public String getDisplayName() { return displayName; }
    
    public static MonsterRarityData fromCode(int code) {
        for (MonsterRarityData rarity : values()) {
            if (rarity.code == code) {
                return rarity;
            }
        }
        return NORMAL;
    }
    
    /**
     * Determine rarity from object type ID.
     * Based on RSBot MonsterRarity enum logic.
     */
    public static MonsterRarityData fromTypeId(int typeId1, int typeId2, int typeId3) {
        // Champion monsters have specific type pattern
        if (typeId3 == 4) return CHAMPION;
        if (typeId3 == 5) return UNIQUE;
        if (typeId3 == 6) return GIANT;
        if (typeId3 == 7) return TITAN;
        if (typeId3 == 8) return ELITE;
        if (typeId3 == 9) return EVENT;
        return NORMAL;
    }
}
