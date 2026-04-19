package org.sokybot.pet.api;

/**
 * Logical pet role mapped from COS {@code CosType} type ids (server protocol).
 */
public enum PetRole {

    /** Ability / pickup pet (e.g. Monkey, Rabbit grab pet). */
    GRAB_PET,

    /** Growth / attack pet. */
    ATTACK_PET,

    /** Fellow pet. */
    FELLOW_PET,

    /** Transport, unknown COS type, or not applicable. */
    UNKNOWN;

    /**
     * Maps {@code CosType} numeric ids: GROWTH=3, ABILITY_PET=4, FELLOW=9.
     *
     * @param cosTypeId raw type id from the protocol (see game-events {@code CosType})
     * @return mapped role, or {@link #UNKNOWN}
     */
    public static PetRole fromCosType(int cosTypeId) {
        switch (cosTypeId) {
            case 3:
                return ATTACK_PET;
            case 4:
                return GRAB_PET;
            case 9:
                return FELLOW_PET;
            default:
                return UNKNOWN;
        }
    }
}
