package org.sokybot.packetsniffer.api;

/**
 * Registry of packet sniffer instances per machine.
 * Used by scripted pages and MachinePagesActivator to obtain the sniffer for a given machine.
 */
public interface IPacketSnifferRegistry {

    /**
     * Get the packet sniffer page backend for the given machine.
     *
     * @param machineFullName full name of the machine (e.g. group/machine)
     * @return the sniffer page, or null if no sniffer is registered for that machine
     */
    IPacketSnifferPage getSniffer(String machineFullName);
}
