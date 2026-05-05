package org.sokybot.behaviors.logistics.router;

import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Inbound server trade opcodes (0xB081–0xB084). One {@link IPacketTranslator} per opcode.
 * Delegates to {@link FieldTradeBehavior#notifyInboundTradeOpcode}.
 */
final class RouterTradePacketListener {

    private RouterTradePacketListener() {
    }

    private static List<IGameEvent> dispatch(String machineFullName, int opcode) {
        FieldTradeBehavior.notifyInboundTradeOpcode(machineFullName, opcode);
        return Collections.emptyList();
    }

    @Component(service = IPacketTranslator.class, immediate = true)
    public static final class B081 implements IPacketTranslator {
        @Override
        public int getOpcode() {
            return 0xB081;
        }

        @Override
        public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet, ChunkedPacketManager chunkManager) {
            return dispatch(machineFullName, 0xB081);
        }
    }

    @Component(service = IPacketTranslator.class, immediate = true)
    public static final class B082 implements IPacketTranslator {
        @Override
        public int getOpcode() {
            return 0xB082;
        }

        @Override
        public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet, ChunkedPacketManager chunkManager) {
            return dispatch(machineFullName, 0xB082);
        }
    }

    @Component(service = IPacketTranslator.class, immediate = true)
    public static final class B083 implements IPacketTranslator {
        @Override
        public int getOpcode() {
            return 0xB083;
        }

        @Override
        public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet, ChunkedPacketManager chunkManager) {
            return dispatch(machineFullName, 0xB083);
        }
    }

    @Component(service = IPacketTranslator.class, immediate = true)
    public static final class B084 implements IPacketTranslator {
        @Override
        public int getOpcode() {
            return 0xB084;
        }

        @Override
        public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet, ChunkedPacketManager chunkManager) {
            return dispatch(machineFullName, 0xB084);
        }
    }
}
