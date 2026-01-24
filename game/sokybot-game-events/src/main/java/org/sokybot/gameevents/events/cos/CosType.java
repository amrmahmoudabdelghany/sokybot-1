package org.sokybot.gameevents.events.cos;

/**
 * Defines the different types of COS (controlled objects).
 */
public enum CosType {
    TRANSPORT(1),      // Normal mount/vehicle
    JOB_TRANSPORT(2),  // Trade transport
    GROWTH(3),         // Growth pet (attack pet)
    ABILITY_PET(4),    // Ability pet (pickup pet)
    FELLOW(9);         // Fellow pet
    
    private final int typeId;
    
    CosType(int typeId) {
        this.typeId = typeId;
    }
    
    public int getTypeId() {
        return typeId;
    }
    
    public static CosType fromTypeId(int typeId) {
        for (CosType type : values()) {
            if (type.typeId == typeId) {
                return type;
            }
        }
        return null;
    }
}
