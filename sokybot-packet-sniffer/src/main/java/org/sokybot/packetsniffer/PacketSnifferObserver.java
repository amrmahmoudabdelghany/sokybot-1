package org.sokybot.packetsniffer;

import org.sokybot.network.IPacketObserver;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Packet observer that forwards packets to PacketSnifferService
 */
public class PacketSnifferObserver implements IPacketObserver {
    
    private final PacketSnifferService service;
    private final String machineName;
    
    public PacketSnifferObserver(PacketSnifferService service, String machineName) {
        this.service = service;
        this.machineName = machineName;
    }
    
    @Override
    public void onNext(int opcode, ImmutablePacket packet) {
        service.display(packet);
    }
    
    @Override
    public void onComplete() {
        // Handle completion if needed
    }
    
    @Override
    public void onError(Throwable ex) {
        System.err.println("Packet observer error for " + machineName + ": " + ex.getMessage());
        ex.printStackTrace();
    }
}
