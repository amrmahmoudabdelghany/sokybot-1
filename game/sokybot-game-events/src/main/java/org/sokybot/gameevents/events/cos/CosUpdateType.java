package org.sokybot.gameevents.events.cos;

/**
 * Defines the types of COS updates.
 */
public enum CosUpdateType {
    TERMINATE(1),
    INVENTORY(2),
    EXPERIENCE(3),
    HUNGER(4),       // Also used for Fellow satiety
    NAME_CHANGE(5),
    MODEL_CHANGE(7),
    FELLOW_KILL_EXP(8);
    
    private final int typeId;
    
    CosUpdateType(int typeId) {
        this.typeId = typeId;
    }
    
    public int getTypeId() {
        return typeId;
    }
    
    public static CosUpdateType fromTypeId(int typeId) {
        for (CosUpdateType type : values()) {
            if (type.typeId == typeId) {
                return type;
            }
        }
        return null;
    }
}
