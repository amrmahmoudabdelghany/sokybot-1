package org.sokybot.engine.core.dispatcher;

import org.sokybot.engine.api.DispatchException;
import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.PacketSerializationException;
import org.sokybot.engine.api.RateLimitException;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.MutablePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of dispatcher that wraps IProxyConnection.
 * Provides framework-agnostic packet sending interface.
 */
public class DispatcherImpl implements IDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DispatcherImpl.class);

    private final IProxyConnection proxyConnection;
    private final String machineId;
    private final AtomicLong lastAgentRequestAtMs = new AtomicLong(0L);
    private final AtomicBoolean coldStartBypassUsed = new AtomicBoolean(false);
    private static final long AGENT_REQUEST_MIN_INTERVAL_MS = 10_000L;

    public DispatcherImpl(IProxyConnection proxyConnection, String machineId) {
        if (proxyConnection == null) {
            throw new IllegalArgumentException("Proxy connection cannot be null");
        }
        this.proxyConnection = proxyConnection;
        this.machineId = machineId;
    }

    @Override
    public void sendToServer(MutablePacket packet) {
        if (packet == null) {
            throw new IllegalArgumentException("Packet cannot be null");
        }

        try {
            if (!proxyConnection.isServerConnected()) {
                throw new DispatchException("Not connected to server for machine: " + machineId);
            }

            // Defensive deep snapshot to avoid post-dispatch mutations racing async IO.
            MutablePacket immutableSnapshot = MutablePacket.wrap(java.util.Arrays.copyOf(packet.unwrap(), packet.unwrap().length));
            proxyConnection.sendToServer(immutableSnapshot);

        } catch (Exception e) {
            if (e instanceof DispatchException) {
                throw e;
            }
            throw new DispatchException("Failed to send packet to server: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendToClient(MutablePacket packet) {
        if (packet == null) {
            throw new IllegalArgumentException("Packet cannot be null");
        }

        try {
            if (!proxyConnection.isClientConnected()) {
                throw new DispatchException("Not connected to client for machine: " + machineId);
            }

            MutablePacket immutableSnapshot = MutablePacket.wrap(java.util.Arrays.copyOf(packet.unwrap(), packet.unwrap().length));
            proxyConnection.sendToClient(immutableSnapshot);

        } catch (Exception e) {
            if (e instanceof DispatchException) {
                throw e;
            }
            throw new DispatchException("Failed to send packet to client: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isConnected() {
        return proxyConnection.isConnected();
    }

    @Override
    public boolean isClientConnected() {
        return proxyConnection.isClientConnected();
    }

    @Override
    public boolean isServerConnected() {
        return proxyConnection.isServerConnected();
    }

    @Override
    public void connect(String host, int port) {
        try {
            log.info("Dispatcher connecting to {}:{}", host, port);
            proxyConnection.connectToServer(host, port);
        } catch (Exception e) {
            throw new DispatchException("Failed to connect to " + host + ":" + port, e);
        }
    }

    @Override
    public void setClientlessMode(boolean clientlessMode) {
        log.info("Dispatcher setting clientless mode: {}", clientlessMode);
        proxyConnection.setClientlessMode(clientlessMode);
    }

    @Override
    public boolean isClientlessMode() {
        return proxyConnection.isClientlessMode();
    }

    @Override
    public void disconnect() {
        try {
            log.info("Dispatcher disconnecting");
            proxyConnection.disconnect();
        } catch (Exception e) {
            log.warn("Error during disconnect", e);
        }
    }

    @Override
    public void sendLoginRequest(byte locale, String username, String password, int agentId, String charsetName) {
        if (agentId < 0) {
            throw new PacketSerializationException("agentId must be non-negative uint32-compatible value");
        }
        try {
            Charset cs = Charset.forName(charsetName == null || charsetName.isBlank() ? "UTF-8" : charsetName);
            byte[] userBytes = String.valueOf(username == null ? "" : username).getBytes(cs);
            byte[] passBytes = String.valueOf(password == null ? "" : password).getBytes(cs);
            int packetLen = 1 + 2 + userBytes.length + 2 + passBytes.length + 4;
            MutablePacket packet = MutablePacket.getBuilder(packetLen, ClientOpcode.LOGIN_REQUEST)
                    .put(locale)
                    .putShort((short) userBytes.length)
                    .putBytes(userBytes)
                    .putShort((short) passBytes.length)
                    .putBytes(passBytes)
                    .putInt(agentId)
                    .build();
            sendToServer(packet);
        } catch (DispatchException e) {
            throw e;
        } catch (Exception e) {
            throw new PacketSerializationException("Failed to serialize/send login request", e);
        }
    }

    @Override
    public boolean sendAgentRequest(boolean allowColdStartBypass) {
        long now = System.currentTimeMillis();
        long last = lastAgentRequestAtMs.get();
        boolean bypass = allowColdStartBypass && !coldStartBypassUsed.get();
        if (!bypass && last > 0L && now - last < AGENT_REQUEST_MIN_INTERVAL_MS) {
            throw new RateLimitException("Agent request blocked by rate limit window");
        }
        try {
            MutablePacket packet = MutablePacket.getBuilder(0, ClientOpcode.AGENT_REQUEST).build();
            sendToServer(packet);
            lastAgentRequestAtMs.set(now);
            if (bypass) {
                coldStartBypassUsed.set(true);
            }
            return true;
        } catch (DispatchException e) {
            // Do not consume bypass token on transport failures.
            throw e;
        }
    }

    @Override
    public void sendLogoutRequest() {
        try {
            MutablePacket packet = MutablePacket.getBuilder(0, ClientOpcode.LOGOUT_REQUEST).build();
            sendToServer(packet);
        } catch (DispatchException e) {
            throw e;
        } catch (Exception e) {
            throw new PacketSerializationException("Failed to serialize/send logout request", e);
        }
    }
}
