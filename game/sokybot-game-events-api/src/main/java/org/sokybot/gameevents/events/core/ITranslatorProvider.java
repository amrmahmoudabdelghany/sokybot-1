package org.sokybot.gameevents.events.core;

import java.util.Set;

import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Provider interface for creating translator instances.
 * Enables plugin-based extension following Open/Closed Principle.
 * 
 * <p>Implementations register as OSGi services and are discovered dynamically
 * by ExtensibleTranslatorFactory. This allows custom packets from private servers
 * to be supported without modifying core code.
 * 
 * <p>Translators are created per-game (shared across all bots) for memory optimization.
 * Each translator instance gets the game's IGameDataLookup injected.
 */
public interface ITranslatorProvider {
    
    /**
     * Check if this provider can create a translator for the given opcode.
     * 
     * @param opcode The packet opcode
     * @param lookup Game-specific lookup (contains version, gamePath, etc.)
     * @return true if this provider handles the opcode
     */
    boolean supports(int opcode, IGameDataLookup lookup);
    
    /**
     * Create a translator instance for the given opcode.
     * Called once per-game (shared across all bots).
     * 
     * <p>Translators must be thread-safe since they're shared across bots.
     * Per-bot state (like ChunkedPacketManager) is passed via translate() method.
     * 
     * @param opcode The packet opcode
     * @param lookup Game-specific lookup service
     * @return New translator instance, or null if not supported
     */
    IPacketTranslator createTranslator(int opcode, IGameDataLookup lookup);
    
    /**
     * Get priority for conflict resolution.
     * Higher priority providers override lower priority ones when multiple
     * providers support the same opcode.
     * 
     * <p>Priority guidelines:
     * <ul>
     *   <li>Core translators: 100 (highest)</li>
     *   <li>Version-aware: 75</li>
     *   <li>Custom/plugins: 50 (default)</li>
     *   <li>Fallback: 25 (lowest)</li>
     * </ul>
     * 
     * @return Priority value (higher = more important)
     */
    default int getPriority() {
        return 50;
    }
    
    /**
     * Get list of opcodes this provider supports.
     * Used for optimization - factory can pre-filter providers.
     * 
     * <p>If null is returned, factory will call supports() for each opcode
     * in the common range (less efficient but more flexible).
     * 
     * @return Set of supported opcodes, or null if unknown/dynamic
     */
    default Set<Integer> getSupportedOpcodes() {
        return null; // Unknown - factory will query per opcode
    }
}
