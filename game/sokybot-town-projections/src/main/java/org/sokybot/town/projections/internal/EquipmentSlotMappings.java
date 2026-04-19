package org.sokybot.town.projections.internal;

import java.util.Optional;

import org.sokybot.gameevents.enums.EquipmentSlot;
import org.sokybot.town.api.EquipSlot;

/**
 * Maps Silkroad equipment indices to {@link EquipSlot}.
 */
final class EquipmentSlotMappings {

    private EquipmentSlotMappings() {
    }

    static Optional<EquipSlot> fromRawSlot(int raw) {
        byte b = (byte) raw;
        for (EquipmentSlot es : EquipmentSlot.values()) {
            if (es.getSlot() == b) {
                return Optional.of(map(es));
            }
        }
        return Optional.empty();
    }

    private static EquipSlot map(EquipmentSlot es) {
        switch (es) {
            case Helm:
                return EquipSlot.HEAD;
            case Mail:
                return EquipSlot.CHEST;
            case Shoulder:
                return EquipSlot.SHOULDERS;
            case Gauntlet:
                return EquipSlot.GLOVES;
            case Pants:
                return EquipSlot.PANTS;
            case Boots:
                return EquipSlot.BOOTS;
            case Primary:
                return EquipSlot.MAIN_HAND;
            case Secondary:
                return EquipSlot.OFF_HAND;
            case Earring:
                return EquipSlot.EARRING_LEFT;
            case Necklace:
                return EquipSlot.NECKLACE;
            case Ring1:
                return EquipSlot.RING_LEFT;
            case Ring2:
                return EquipSlot.RING_RIGHT;
            case Extra:
                return EquipSlot.CHEST;
            default:
                return EquipSlot.CHEST;
        }
    }
}
