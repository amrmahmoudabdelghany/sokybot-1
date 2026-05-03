package org.sokybot.combat.api.shapeshifter;

/**
 * Epic #22 Shapeshifter Protocol: equip a weapon by catalogue ref id for role flexing.
 */
public interface IWeaponFlexService {

    /**
     * Moves a stack from inventory (backpack slots {@code >= 13}) into primary weapon slot {@code 6}.
     *
     * @return {@code true} if a matching stack was found and the move packet was sent
     */
    boolean equipWeaponByRefId(String machineFullName, int targetWeaponRefId);

    /**
     * Ref id of the item currently in equip slot 6, or {@code 0} if empty/unknown.
     */
    int getCurrentWeaponRefId(String machineFullName);
}
