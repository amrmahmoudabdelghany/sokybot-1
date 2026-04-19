package org.sokybot.combat.api;

/**
 * Latest ammunition count projection per {@code machineFullName} (from inventory ammo events).
 */
public interface IAmmoMonitor {

    /**
     * Latest known ammo units for the machine, or {@code -1} when unknown (no update yet).
     */
    int getAmmoCount(String machineFullName);

    /**
     * {@code false} only when ammo is known and zero; {@code true} when unknown (fail-open) or count &gt; 0.
     */
    boolean hasAmmo(String machineFullName);
}
