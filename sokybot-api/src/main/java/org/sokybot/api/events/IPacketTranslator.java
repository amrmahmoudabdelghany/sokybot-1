package org.sokybot.api.events;

import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates raw network packets to typed domain events.
 * Implementations are registered as OSGi services and discovered dynamically.
 */
public interface IPacketTranslator {
    
    /**
     * Gets the opcode this translator handles.
     * @return The packet opcode (e.g., 0x3015 for ENTITY_SPAWN)
     */
    int getOpcode();
    
    /**
     * Translates a raw packet to a domain event.
     * 
     * @param machineFullName The full name of the machine that received this packet
     * @param packet The raw packet data
     * @return The translated game event, or null if packet couldn't be translated
     */
    IGameEvent translate(String machineFullName, ImmutablePacket packet);
}
