package org.sokybot.packetsniffer;

import org.sokybot.network.IPacketObserver;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.proxy.recording.IPacketRecorder;
import org.sokybot.proxy.recording.PacketDirection;
import org.sokybot.proxy.recording.RecordedPacket;

/**
 * Packet observer that forwards packets to PacketSnifferService
 */
public class PacketSnifferObserver implements IPacketObserver {
    
    private final PacketSnifferService service;
    private final String machineName;
    private final IPacketRecorder recorder;
    
    public PacketSnifferObserver(PacketSnifferService service, String machineName, IPacketRecorder recorder) {
        this.service = service;
        this.machineName = machineName;
        this.recorder = recorder;
    }
    
    @Override
    public void onPacket(ImmutablePacket packet) {
        if (recorder != null && recorder.isRecording(machineName)) {
            PacketDirection direction = packet.getPacketSource() == NetworkPeer.SERVER
                    ? PacketDirection.TO_CLIENT
                    : PacketDirection.TO_SERVER;
            recorder.recordPacket(machineName, RecordedPacket.now(direction, packet.getOpcode(), packet.toBytes()));
        }
        service.display(packet);
    }
}
