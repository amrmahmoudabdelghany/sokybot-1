package org.sokybot.gameevents.events.core;

import java.util.List;

import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates raw network packets to typed domain events.
 * 
 * <p>
 * Translators are created per-game (shared across all bots) for memory
 * optimization.
 * They must be thread-safe since multiple bots may use the same translator
 * instance concurrently.
 * 
 * <p>
 * Per-bot state (like ChunkedPacketManager) is passed via translate() method
 * parameter.
 */
public interface IPacketTranslator {

    /**
     * Gets the opcode this translator handles.
     * 
     * @return The packet opcode (e.g., 0x3015 for ENTITY_SPAWN)
     */
    int getOpcode();

    /**
     * Translates a raw packet to domain events.
     * May return multiple events for packets that contain multiple entities
     * (e.g., character data with items, skills, buffs).
     * 
     * <p>
     * This method is thread-safe - translators are shared across bots.
     * Per-bot state (like ChunkedPacketManager) is accessed via registry.
     * 
     * @param machineFullName The full name of the machine that received this packet
     * @param packet          The raw packet data
     * @param chunkManager    Not used - kept for backward compatibility
     *                        (translators use registry)
     * @return List of translated game events, or empty list if packet couldn't be
     *         translated
     */
    List<IGameEvent> translate(String machineFullName, ImmutablePacket packet,
            ChunkedPacketManager chunkManager);

}
