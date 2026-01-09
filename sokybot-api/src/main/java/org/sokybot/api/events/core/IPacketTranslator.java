package org.sokybot.api.events;

import java.util.List;

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
     * Translates a raw packet to domain events.
     * May return multiple events for packets that contain multiple entities
     * (e.g., character data with items, skills, buffs).
     * 
     * @param machineFullName The full name of the machine that received this packet
     * @param packet The raw packet data
     * @return List of translated game events, or empty list if packet couldn't be translated
     */
    List<IGameEvent> translate(String machineFullName, ImmutablePacket packet);
}

