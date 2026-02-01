package org.sokybot.proxy.recording;

/**
 * Direction of a recorded packet.
 */
public enum PacketDirection {

    /**
     * Packet sent from client to server.
     */
    TO_SERVER,

    /**
     * Packet sent from server to client.
     */
    TO_CLIENT
}
