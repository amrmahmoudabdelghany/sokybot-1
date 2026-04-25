package org.sokybot.engine.test.util.mocks;

import org.sokybot.network.IPacketObserver;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.IPacketSubscription;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.proxy.IProxyConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of IProxyConnection for testing.
 * Tracks connection state and sent packets.
 */
public class MockProxyConnection implements IProxyConnection {

    @Override
    public void setConnectionListener(org.sokybot.proxy.IConnectionListener listener) {
    }

    @Override
    public boolean isClientlessMode() {
        return false;
    }

    @Override
    public void setClientlessMode(boolean clientlessMode) {
    }

    private boolean connected = false;
    private boolean clientConnected = false;
    private boolean serverConnected = false;
    private final List<MutablePacket> sentToServer = new ArrayList<>();
    private final List<MutablePacket> sentToClient = new ArrayList<>();
    private int localServerPort = -1;
    private IPacketPublisher packetPublisher;

    /**
     * Creates a new MockProxyConnection.
     */
    public MockProxyConnection() {
        this.packetPublisher = new NoOpPacketPublisher();
    }

    /**
     * Sets the connection state.
     */
    public MockProxyConnection withConnected(boolean connected) {
        this.connected = connected;
        this.clientConnected = connected;
        this.serverConnected = connected;
        return this;
    }

    /**
     * Sets the packet publisher.
     */
    public MockProxyConnection withPacketPublisher(IPacketPublisher publisher) {
        this.packetPublisher = publisher;
        return this;
    }

    @Override
    public void startLocalServer(int port) {
        this.localServerPort = port;
        this.clientConnected = true;
        updateConnectedState();
    }

    @Override
    public void connectToServer(String host, int port) {
        this.serverConnected = true;
        updateConnectedState();
    }

    @Override
    public void disconnect() {
        this.connected = false;
        this.clientConnected = false;
        this.serverConnected = false;
    }

    @Override
    public void sendToServer(MutablePacket packet) {
        sentToServer.add(packet);
    }

    @Override
    public void sendToClient(MutablePacket packet) {
        sentToClient.add(packet);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean isClientConnected() {
        return clientConnected;
    }

    @Override
    public boolean isServerConnected() {
        return serverConnected;
    }

    @Override
    public IPacketPublisher getPacketPublisher() {
        return packetPublisher;
    }

    private void updateConnectedState() {
        this.connected = clientConnected && serverConnected;
    }

    /**
     * Gets all packets sent to server.
     */
    public List<MutablePacket> getSentToServer() {
        return new ArrayList<>(sentToServer);
    }

    /**
     * Gets all packets sent to client.
     */
    public List<MutablePacket> getSentToClient() {
        return new ArrayList<>(sentToClient);
    }

    /**
     * Clears all sent packets.
     */
    public void clear() {
        sentToServer.clear();
        sentToClient.clear();
    }

    /**
     * Gets the local server port (if started).
     */
    public int getLocalServerPort() {
        return localServerPort;
    }

    @Override
    public String getMachineId() {
        return "mock-machine-id";
    }

    /** No-op IPacketPublisher stub — all subscribe methods return a no-op subscription. */
    private static class NoOpPacketPublisher implements IPacketPublisher {
        private static final IPacketSubscription NOOP = () -> {};

        @Override
        public IPacketSubscription subscribe(int opcode, IPacketObserver observer) { return NOOP; }

        @Override
        public IPacketSubscription subscribe(IPacketObserver observer, int... opcodes) { return NOOP; }

        @Override
        public IPacketSubscription subscribeAll(IPacketObserver observer) { return NOOP; }
    }
}
