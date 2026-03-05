package org.sokybot.engine.test.util.mocks;

import org.mockito.Mockito;
import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.DispatchException;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of IDispatcher for testing.
 * Tracks sent packets and connection state.
 */
public class MockDispatcher implements IDispatcher {

    private final List<Object> sentToServer = new ArrayList<>();
    private final List<Object> sentToClient = new ArrayList<>();
    private boolean connected = false;
    private boolean clientConnected = false;
    private boolean serverConnected = false;
    private DispatchException throwExceptionOnSend = null;

    /**
     * Creates a new MockDispatcher.
     */
    public MockDispatcher() {
    }

    /**
     * Sets the connection state.
     */
    public MockDispatcher withConnected(boolean connected) {
        this.connected = connected;
        this.clientConnected = connected;
        this.serverConnected = connected;
        return this;
    }

    /**
     * Sets client connection state.
     */
    public MockDispatcher withClientConnected(boolean clientConnected) {
        this.clientConnected = clientConnected;
        updateConnectedState();
        return this;
    }

    /**
     * Sets server connection state.
     */
    public MockDispatcher withServerConnected(boolean serverConnected) {
        this.serverConnected = serverConnected;
        updateConnectedState();
        return this;
    }

    /**
     * Sets an exception to throw on send operations.
     */
    public MockDispatcher withExceptionOnSend(DispatchException exception) {
        this.throwExceptionOnSend = exception;
        return this;
    }

    private void updateConnectedState() {
        this.connected = clientConnected && serverConnected;
    }

    @Override
    public void sendToServer(Object packet) throws DispatchException {
        if (throwExceptionOnSend != null) {
            throw throwExceptionOnSend;
        }
        sentToServer.add(packet);
    }

    @Override
    public void sendToClient(Object packet) throws DispatchException {
        if (throwExceptionOnSend != null) {
            throw throwExceptionOnSend;
        }
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

    /**
     * Gets all packets sent to server.
     */
    public List<Object> getSentToServer() {
        return new ArrayList<>(sentToServer);
    }

    /**
     * Gets all packets sent to client.
     */
    public List<Object> getSentToClient() {
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
     * Gets the number of packets sent to server.
     */
    public int getServerPacketCount() {
        return sentToServer.size();
    }

    /**
     * Gets the number of packets sent to client.
     */
    public int getClientPacketCount() {
        return sentToClient.size();
    }

    @Override
    public void connect(String host, int port) {
        this.connected = true;
        this.clientConnected = true;
        this.serverConnected = true;
    }

    @Override
    public void disconnect() {
        this.connected = false;
        this.clientConnected = false;
        this.serverConnected = false;
    }

    private boolean clientlessMode = false;

    @Override
    public void setClientlessMode(boolean clientlessMode) {
        this.clientlessMode = clientlessMode;
    }

    @Override
    public boolean isClientlessMode() {
        return clientlessMode;
    }
}
