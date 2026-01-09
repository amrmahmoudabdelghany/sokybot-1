package org.sokybot.pk2extractor.dto;

/**
 * Monster rarity classification enum.
 */
public enum MonsterRarityData {
    
    NORMAL(0, "Normal"),
    CHAMPION(1, "Champion"),
    UNIQUE(2, "Unique"),
    GIANT(3, "Giant"),
    TITAN(4, "Titan"),
    ELITE(5, "Elite"),
    EVENT(6, "Event");
    
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
    
    public static MonsterRarityData fromTypeId(int typeId1, int typeId2, int typeId3) {
        if (typeId3 == 4) return CHAMPION;
        if (typeId3 == 5) return UNIQUE;
        if (typeId3 == 6) return GIANT;
        if (typeId3 == 7) return TITAN;
        if (typeId3 == 8) return ELITE;
        if (typeId3 == 9) return EVENT;
        return NORMAL;
    }
}
