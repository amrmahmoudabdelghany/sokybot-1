package org.sokybot.gameevents.script;

import java.util.Collections;
import java.util.List;

import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

import groovy.lang.Closure;

/**
 * Adapts a Groovy closure into an {@link IPacketTranslator}.
 * At translate time the closure is cloned, its delegate is set to a
 * TranslatorContext (with lookup, singleEvent, noEvents, getChunkManager),
 * then the closure is called with (machineFullName, packet).
 */
public class GroovyTranslator implements IPacketTranslator {

    private final int opcode;
    private final Closure<?> closure;
    private final IGameDataLookup lookup;

    public GroovyTranslator(int opcode, Closure<?> closure, IGameDataLookup lookup) {
        this.opcode = opcode;
        this.closure = closure;
        this.lookup = lookup;
    }

    @Override
    public int getOpcode() {
        return opcode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet,
            ChunkedPacketManager chunkManager) {
        try {
            Closure<?> clone = closure.rehydrate(null, new TranslatorContext(lookup), closure.getThisObject());
            clone.setResolveStrategy(Closure.DELEGATE_FIRST);
            Object result = clone.call(machineFullName, packet);
            if (result instanceof List) {
                return (List<IGameEvent>) result;
            }
            if (result instanceof IGameEvent) {
                return List.of((IGameEvent) result);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
