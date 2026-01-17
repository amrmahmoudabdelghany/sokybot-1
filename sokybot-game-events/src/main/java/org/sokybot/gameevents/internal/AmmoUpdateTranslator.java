package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.inventory.AmmoUpdateEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates ammunition update packets (opcode 0x3201) to AmmoUpdateEvent.
 * Reference: RSBot InventoryUpdateAmmoResponse = 0x3201
 */
public class AmmoUpdateTranslator extends AbstractTranslator {
    
    private static final int AMMO_UPDATE_OPCODE = 0x3201;
    public AmmoUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return AMMO_UPDATE_OPCODE;
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int ammoCount = reader.getShort() & 0xFFFF;
            return singleEvent(new AmmoUpdateEvent(machineFullName, ammoCount));
        } catch (Exception e) {
            return noEvents();
        }
}
