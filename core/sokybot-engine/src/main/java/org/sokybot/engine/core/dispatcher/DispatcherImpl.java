package org.sokybot.engine.core.dispatcher;

import org.sokybot.engine.api.DispatchException;
import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.PacketSerializationException;
import org.sokybot.engine.api.RateLimitException;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.network.NetworkPeer;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of dispatcher that wraps IProxyConnection.
 * Provides framework-agnostic packet sending interface.
 *
 * <p>Gateway login (0x6102) deduplication is enforced in the proxy {@code PacketEncoder} (game-server
 * channel), not in this class.
 *
 * <p>Human pacing after 0xA101: {@link #sendLoginRequest} may block the calling thread until at least
 * {@code gatewayLoginPauseAfterAgentListMs} has elapsed since either {@link org.sokybot.gamemodel.LoginState}
 * recorded the list (translator path) or the proxy observed 0xA101 on the game-server channel (always deployed
 * with {@link org.sokybot.proxy.ProxyConnection}).
 */
public class DispatcherImpl implements IDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DispatcherImpl.class);

    private static final String RUNTIME_LOGIN_SETTINGS_KEY = "runtimeLoginSettings";
    private static final String GATEWAY_LOGIN_PAUSE_AFTER_AGENT_LIST_MS_KEY = "gatewayLoginPauseAfterAgentListMs";
    private static final long DEFAULT_GATEWAY_LOGIN_PAUSE_AFTER_AGENT_LIST_MS = 1500L;
    private static final long MAX_GATEWAY_AGENT_LIST_PACING_STALE_MS = 90_000L;

    private final IProxyConnection proxyConnection;
    private final String machineId;
    /** Set after {@link org.sokybot.engine.core.workflow.WorkflowContextImpl} is constructed (circular init). */
    private volatile IWorkflowContext workflowContext;
    private final IGameModel gameModel;
    private final AtomicLong lastAgentRequestAtMs = new AtomicLong(0L);
    private final AtomicBoolean coldStartBypassUsed = new AtomicBoolean(false);
    private static final long AGENT_REQUEST_MIN_INTERVAL_MS = 10_000L;

    static String hexPrefix(byte[] raw, int maxBytes) {
        if (raw == null || raw.length == 0 || maxBytes <= 0) {
            return "";
        }
        int n = Math.min(raw.length, maxBytes);
        StringBuilder sb = new StringBuilder(n * 3);
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", raw[i] & 0xFF));
        }
        if (raw.length > n) {
            sb.append(" ...");
        }
        return sb.toString();
    }

    public DispatcherImpl(IProxyConnection proxyConnection, String machineId) {
        this(proxyConnection, machineId, null);
    }

    /**
     * @param gameModel used for gateway 0xA101→0x6102 wall-clock pacing; may be null in tests
     */
    public DispatcherImpl(IProxyConnection proxyConnection, String machineId, IGameModel gameModel) {
        if (proxyConnection == null) {
            throw new IllegalArgumentException("Proxy connection cannot be null");
        }
        this.proxyConnection = proxyConnection;
        this.machineId = machineId;
        this.gameModel = gameModel;
    }

    /**
     * Wires workflow persistent settings for login pause ms (called once from {@link org.sokybot.engine.core.EngineCore}).
     */
    public void bindWorkflowContext(IWorkflowContext workflowContext) {
        this.workflowContext = workflowContext;
    }

    private static long effectiveGatewayLoginPauseAfterAgentListMs(IWorkflowContext ctx) {
        if (ctx != null && ctx.getPersistentData() != null) {
            Object snap = ctx.getPersistentData().get(RUNTIME_LOGIN_SETTINGS_KEY);
            if (snap instanceof Map<?, ?>) {
                Object v = ((Map<?, ?>) snap).get(GATEWAY_LOGIN_PAUSE_AFTER_AGENT_LIST_MS_KEY);
                if (v instanceof Number) {
                    long ms = ((Number) v).longValue();
                    return Math.max(0L, Math.min(60_000L, ms));
                }
            }
        }
        String prop = System.getProperty("sokybot.login.pauseAfterAgentListMs");
        if (prop != null && !prop.isBlank()) {
            try {
                long parsed = Long.parseLong(prop.trim());
                return Math.max(0L, Math.min(60_000L, parsed));
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_GATEWAY_LOGIN_PAUSE_AFTER_AGENT_LIST_MS;
    }

    @Override
    public void awaitGatewayLoginPauseAfterAgentList() {
        sleepRemainingPauseAfterAgentList();
    }

    private long resolveLastAgentListObservedWallClockMs() {
        long modelTs = 0L;
        if (gameModel != null) {
            modelTs = gameModel.getLoginState().getLastGatewayAgentListObservedWallClockMs();
        }
        long proxyTs = proxyConnection.getLastGatewayAgentListFromServerWallClockMs();
        return Math.max(modelTs, proxyTs);
    }

    private void sleepRemainingPauseAfterAgentList() {
        long pauseMs = effectiveGatewayLoginPauseAfterAgentListMs(workflowContext);
        if (pauseMs <= 0L) {
            return;
        }
        long observed = resolveLastAgentListObservedWallClockMs();
        if (observed <= 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        long age = now - observed;
        if (age < 0L || age > MAX_GATEWAY_AGENT_LIST_PACING_STALE_MS) {
            return;
        }
        long wait = pauseMs - age;
        if (wait <= 0L) {
            return;
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("[machine={}] Gateway 0x6102 pacing: sleeping {} ms (pauseMs={}, ageSinceA101={} ms)",
                        machineId, wait, pauseMs, age);
            }
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[machine={}] Gateway 0x6102 pacing sleep interrupted", machineId);
        }
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
            MutablePacket immutableSnapshot =
                    MutablePacket.wrap(java.util.Arrays.copyOf(packet.unwrap(), packet.unwrap().length));
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
        if (agentId < 0 || agentId > 0xFFFF) {
            throw new PacketSerializationException("agentId must be a ushort (0..65535) for gateway 0x6102");
        }
        // vSRO expects locale 22 (0x16). 0 often comes from settings/UI; Groovy toInt(0,22) still yields 0.
        byte localeOnWire = locale;
        if ((localeOnWire & 0xFF) == 0) {
            localeOnWire = 22;
            log.warn("[machine={}] Gateway 0x6102 locale was 0; coercing to 22 (vSRO)", machineId);
        }
        awaitGatewayLoginPauseAfterAgentList();
        try {
            // Align with Login.groovy / typical Silkroad gateways when UI omits charset.
            Charset cs = Charset.forName(charsetName == null || charsetName.isBlank() ? "windows-1252" : charsetName);
            byte[] userBytes = String.valueOf(username == null ? "" : username).getBytes(cs);
            byte[] passBytes = String.valueOf(password == null ? "" : password).getBytes(cs);
            // vSRO / common gateways expect a 2-byte agent id tail (matches Login.groovy fallback).
            int packetLen = 1 + 2 + userBytes.length + 2 + passBytes.length + 2;
            MutablePacket packet = MutablePacket.getBuilder(packetLen, ClientOpcode.LOGIN_REQUEST)
                    .packetEncoding(Encoding.ENCRYPTED)
                    .dataEncoding(Encoding.PLAIN)
                    .packetSource(NetworkPeer.BOT)
                    .put(localeOnWire)
                    .putShort((short) userBytes.length)
                    .putBytes(userBytes)
                    .putShort((short) passBytes.length)
                    .putBytes(passBytes)
                    .putShort((short) agentId)
                    .build();
            if (log.isInfoEnabled()) {
                log.info("[machine={}] Sending gateway 0x6102 (locale=0x{}, agentId={})",
                        machineId, String.format("%02X", localeOnWire & 0xFF), agentId);
            }
            // Compare with packet captures: index 6 is first payload byte (locale) after 6-byte header.
            if (log.isInfoEnabled()) {
                byte[] raw = packet.unwrap();
                log.info("[machine={}] 0x6102 wire preview ({} bytes, hex): {}",
                        machineId, raw.length, hexPrefix(raw, Math.min(raw.length, 40)));
            }
            sendToServer(packet);
            if (gameModel != null) {
                gameModel.getLoginState().setLastGatewayAgentListObservedWallClockMs(0L);
            }
            proxyConnection.clearLastGatewayAgentListFromServerWallClockMs();
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
        if (gameModel != null) {
            // Fresh 0x6101 expects a new 0xA101; do not pace 0x6102 against an older list.
            gameModel.getLoginState().setLastGatewayAgentListObservedWallClockMs(0L);
        }
        proxyConnection.clearLastGatewayAgentListFromServerWallClockMs();
        try {
            MutablePacket packet = MutablePacket.getBuilder(0, ClientOpcode.AGENT_REQUEST)
                    .packetEncoding(Encoding.ENCRYPTED)
                    .dataEncoding(Encoding.PLAIN)
                    .packetSource(NetworkPeer.BOT)
                    .build();
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
            MutablePacket packet = MutablePacket.getBuilder(0, ClientOpcode.LOGOUT_REQUEST)
                    .packetEncoding(Encoding.ENCRYPTED)
                    .dataEncoding(Encoding.PLAIN)
                    .packetSource(NetworkPeer.BOT)
                    .build();
            sendToServer(packet);
        } catch (DispatchException e) {
            throw e;
        } catch (Exception e) {
            throw new PacketSerializationException("Failed to serialize/send logout request", e);
        }
    }
}
