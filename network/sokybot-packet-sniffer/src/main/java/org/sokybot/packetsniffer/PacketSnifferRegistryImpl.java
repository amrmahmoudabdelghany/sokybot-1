package org.sokybot.packetsniffer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sokybot.packetsniffer.api.IPacketSnifferPage;
import org.sokybot.packetsniffer.api.IPacketSnifferRegistry;

/**
 * In-memory registry of packet sniffer instances per machine.
 * Updated by PacketSnifferActivator on machine create/destroy.
 */
public class PacketSnifferRegistryImpl implements IPacketSnifferRegistry {

    private final Map<String, IPacketSnifferPage> sniffers = new ConcurrentHashMap<>();

    public void put(String machineFullName, IPacketSnifferPage sniffer) {
        sniffers.put(machineFullName, sniffer);
    }

    public IPacketSnifferPage remove(String machineFullName) {
        return sniffers.remove(machineFullName);
    }

    @Override
    public IPacketSnifferPage getSniffer(String machineFullName) {
        return sniffers.get(machineFullName);
    }
}
