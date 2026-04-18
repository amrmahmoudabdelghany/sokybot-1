package org.sokybot.login.protocol.builtin;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.RateLimitException;
import org.sokybot.engine.api.login.GatewayCredentials;
import org.sokybot.engine.api.login.IGatewayProtocolEmitter;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.proxy.IProxyConnection;

@Component(service = IGatewayProtocolEmitter.class, immediate = true)
public final class GatewayProtocolEmitterImpl implements IGatewayProtocolEmitter {

    private static final Logger log = LoggerFactory.getLogger(GatewayProtocolEmitterImpl.class);

    private static final String DISPATCHER_IMPL_CLASS_NAME = "org.sokybot.engine.core.dispatcher.DispatcherImpl";
    private static final String KEY_FALLBACK_AGENT_REQUEST_LAST_MS = "loginFallbackAgentRequestLastMs";
    private static final String KEY_FALLBACK_AGENT_COLD_BYPASS_USED = "loginFallbackAgentColdBypassUsed";

    private static final AtomicReference<Boolean> DISPATCHER_DYNAMIC_PACKET_API_OK = new AtomicReference<Boolean>(null);
    private static final AtomicBoolean LOGGED_DISPATCHER_PACKET_API_FAILURE = new AtomicBoolean(false);

    @Override
    public boolean requestAgentList(IWorkflowContext context, boolean allowColdStartBypass, long minIntervalMs) {
        Objects.requireNonNull(context, "context");
        long effectiveIntervalMs = Math.max(0L, minIntervalMs);
        IDispatcher dispatcher = safeDispatcher(context);
        if (dispatcher != null && DISPATCHER_DYNAMIC_PACKET_API_OK.get() != Boolean.FALSE) {
            try {
                boolean result = dispatcher.sendAgentRequest(allowColdStartBypass);
                DISPATCHER_DYNAMIC_PACKET_API_OK.compareAndSet(null, Boolean.TRUE);
                return result;
            } catch (RateLimitException e) {
                log.warn("Agent request rate-limited: {}", e.getMessage());
                return false;
            } catch (Throwable t) {
                if (!isDispatcherPacketApiChainFailure(t)) {
                    rethrow(t);
                }
                DISPATCHER_DYNAMIC_PACKET_API_OK.set(Boolean.FALSE);
                logDispatcherPacketApiFirstFailure(dispatcher, t);
            }
        }
        return sendAgentRequestFallback(context, allowColdStartBypass, effectiveIntervalMs);
    }

    @Override
    public void sendLoginRequest(
            IWorkflowContext context,
            GatewayCredentials credentials,
            byte locale,
            int agentId,
            String charsetName,
            boolean awaitGatewayPause
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(credentials, "credentials");
        byte localeByte = locale == 0 ? (byte) 22 : locale;
        IDispatcher dispatcher = safeDispatcher(context);
        if (dispatcher != null && awaitGatewayPause) {
            awaitGatewayPause(dispatcher);
        }
        if (dispatcher != null && DISPATCHER_DYNAMIC_PACKET_API_OK.get() != Boolean.FALSE) {
            try {
                dispatcher.sendLoginRequest(
                        localeByte,
                        credentials.getUsername(),
                        credentials.getPassword(),
                        agentId & 0xFFFF,
                        charsetName
                );
                DISPATCHER_DYNAMIC_PACKET_API_OK.compareAndSet(null, Boolean.TRUE);
                return;
            } catch (Throwable t) {
                if (!isDispatcherPacketApiChainFailure(t)) {
                    rethrow(t);
                }
                DISPATCHER_DYNAMIC_PACKET_API_OK.set(Boolean.FALSE);
                logDispatcherPacketApiFirstFailure(dispatcher, t);
            }
        }

        Charset charset = resolveCharset(charsetName);
        byte[] usernameBytes = credentials.getUsername().getBytes(charset);
        byte[] passwordBytes = credentials.getPassword().getBytes(charset);
        int packetLen = 7 + usernameBytes.length + passwordBytes.length;
        MutablePacket loginPacket = MutablePacket.getBuilder(packetLen, ClientOpcode.LOGIN_REQUEST)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put(localeByte)
                .putShort((short) usernameBytes.length)
                .putBytes(usernameBytes)
                .putShort((short) passwordBytes.length)
                .putBytes(passwordBytes)
                .putShort((short) (agentId & 0xFFFF))
                .build();
        if (!sendWorkflowServerPacket(context, loginPacket)) {
            throw new IllegalStateException("Failed to send gateway login request packet");
        }
    }

    @Override
    public void sendGatewayImageCodeAnswer(IWorkflowContext context, String answer) {
        Objects.requireNonNull(context, "context");
        String safeAnswer = answer != null ? answer : "";
        byte[] utf16 = safeAnswer.getBytes(StandardCharsets.UTF_16LE);
        if ((utf16.length & 1) != 0) {
            throw new IllegalStateException("UTF-16LE image code byte length must be even");
        }
        int wcharCount = utf16.length / 2;
        if (wcharCount > 65535) {
            throw new IllegalStateException("Image code answer too long");
        }
        MutablePacket pkt = MutablePacket.getBuilder(2 + utf16.length, ClientOpcode.GATEWAY_IMAGE_CODE_ANSWER)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .putShort((short) wcharCount)
                .putBytes(utf16)
                .build();
        if (!sendWorkflowServerPacket(context, pkt)) {
            throw new IllegalStateException("Failed to send gateway image-code answer packet");
        }
    }

    @Override
    public void sendLogoutRequest(IWorkflowContext context) {
        Objects.requireNonNull(context, "context");
        IDispatcher dispatcher = safeDispatcher(context);
        if (dispatcher != null && DISPATCHER_DYNAMIC_PACKET_API_OK.get() != Boolean.FALSE) {
            try {
                dispatcher.sendLogoutRequest();
                DISPATCHER_DYNAMIC_PACKET_API_OK.compareAndSet(null, Boolean.TRUE);
                return;
            } catch (Throwable t) {
                if (!isDispatcherPacketApiChainFailure(t)) {
                    rethrow(t);
                }
                DISPATCHER_DYNAMIC_PACKET_API_OK.set(Boolean.FALSE);
                logDispatcherPacketApiFirstFailure(dispatcher, t);
            }
        }
        MutablePacket logoutPacket = MutablePacket.getBuilder(0, ClientOpcode.LOGOUT_REQUEST)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .build();
        if (!sendWorkflowServerPacket(context, logoutPacket)) {
            throw new IllegalStateException("Failed to send logout request packet");
        }
    }

    @Override
    public void gracefulShutdown(IWorkflowContext context, long logoutAckTimeoutMs) {
        Objects.requireNonNull(context, "context");
        try {
            IDispatcher dispatcher = safeDispatcher(context);
            if (dispatcher != null && dispatcher.isServerConnected()) {
                try {
                    sendLogoutRequest(context);
                } catch (Exception e) {
                    log.warn("Logout packet send failed during graceful shutdown: {}", e.getMessage());
                }
                long timeout = Math.max(500L, logoutAckTimeoutMs);
                long until = System.currentTimeMillis() + timeout;
                while (System.currentTimeMillis() < until) {
                    IDispatcher d = safeDispatcher(context);
                    if (d == null || !d.isServerConnected()) {
                        break;
                    }
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } finally {
            safeDisconnect(context);
        }
    }

    private boolean sendAgentRequestFallback(IWorkflowContext context, boolean allowBypass, long minIntervalMs) {
        Map<String, Object> persistentData = context.getPersistentData();
        if (persistentData == null) {
            log.error("Cannot enforce fallback agent-request interval: persistent data unavailable");
            return false;
        }
        long now = System.currentTimeMillis();
        long last = asLong(persistentData.get(KEY_FALLBACK_AGENT_REQUEST_LAST_MS), 0L);
        boolean bypass = allowBypass && !Boolean.TRUE.equals(persistentData.get(KEY_FALLBACK_AGENT_COLD_BYPASS_USED));
        if (!bypass && last > 0L && now - last < minIntervalMs) {
            long remaining = minIntervalMs - (now - last);
            log.warn("Skipping fallback agent request due to interval gate ({}ms remaining)", remaining);
            return false;
        }
        MutablePacket agentPacket = MutablePacket.getBuilder(0, ClientOpcode.AGENT_REQUEST)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .build();
        if (!sendWorkflowServerPacket(context, agentPacket)) {
            return false;
        }
        persistentData.put(KEY_FALLBACK_AGENT_REQUEST_LAST_MS, Long.valueOf(now));
        if (bypass) {
            persistentData.put(KEY_FALLBACK_AGENT_COLD_BYPASS_USED, Boolean.TRUE);
        }
        return true;
    }

    private boolean sendWorkflowServerPacket(IWorkflowContext context, MutablePacket packet) {
        IDispatcher dispatcher = safeDispatcher(context);
        if (DISPATCHER_DYNAMIC_PACKET_API_OK.get() == Boolean.FALSE) {
            IProxyConnection proxy = resolveWorkflowProxyConnection(context, dispatcher);
            if (proxy != null) {
                proxy.sendToServer(packet);
                return true;
            }
            log.error("Dispatcher packet API disabled and proxy unavailable; cannot send server packet");
            return false;
        }
        if (dispatcher == null) {
            IProxyConnection proxy = resolveWorkflowProxyConnection(context, null);
            if (proxy != null) {
                proxy.sendToServer(packet);
                return true;
            }
            log.error("Dispatcher and proxy unavailable; cannot send server packet");
            return false;
        }
        try {
            dispatcher.sendToServer(packet);
            return true;
        } catch (Throwable t) {
            if (!isDispatcherPacketApiChainFailure(t)) {
                rethrow(t);
            }
            DISPATCHER_DYNAMIC_PACKET_API_OK.set(Boolean.FALSE);
            logDispatcherPacketApiFirstFailure(dispatcher, t);
            IProxyConnection proxy = resolveWorkflowProxyConnection(context, dispatcher);
            if (proxy != null) {
                proxy.sendToServer(packet);
                return true;
            }
            log.error("Dispatcher failed and proxy unavailable; cannot send server packet");
            return false;
        }
    }

    private IDispatcher safeDispatcher(IWorkflowContext context) {
        try {
            return context.getDispatcher();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private IProxyConnection resolveWorkflowProxyConnection(IWorkflowContext context, IDispatcher dispatcher) {
        try {
            IProxyConnection proxy = context.getProxyConnection();
            if (proxy != null) {
                return proxy;
            }
        } catch (Throwable ignored) {
        }
        if (dispatcher == null) {
            return null;
        }
        try {
            Class<?> dispatcherClass = dispatcher.getClass();
            if (DISPATCHER_IMPL_CLASS_NAME.equals(dispatcherClass.getName())) {
                java.lang.reflect.Field field = dispatcherClass.getDeclaredField("proxyConnection");
                field.setAccessible(true);
                Object value = field.get(dispatcher);
                if (value instanceof IProxyConnection) {
                    return (IProxyConnection) value;
                }
            }
        } catch (Throwable t) {
            log.debug("Could not resolve proxyConnection from dispatcher: {}", t.getMessage());
        }
        return null;
    }

    private static boolean isDispatcherPacketApiChainFailure(Throwable t) {
        for (Throwable current = t; current != null; current = current.getCause()) {
            if (current instanceof AbstractMethodError
                    || current instanceof NoSuchMethodError
                    || current instanceof IncompatibleClassChangeError) {
                return true;
            }
            String className = current.getClass().getName();
            if ("groovy.lang.MissingMethodException".equals(className)) {
                return true;
            }
        }
        return false;
    }

    private void logDispatcherPacketApiFirstFailure(IDispatcher dispatcher, Throwable t) {
        if (!LOGGED_DISPATCHER_PACKET_API_FAILURE.compareAndSet(false, true)) {
            return;
        }
        String receiver = dispatcher != null ? dispatcher.getClass().getName() : "null";
        log.error(
                "First dispatcher packet API failure detected; switching to raw proxy packet fallback "
                        + "(receiver={}, message={})",
                receiver,
                t != null ? t.getMessage() : ""
        );
    }

    private void awaitGatewayPause(IDispatcher dispatcher) {
        try {
            dispatcher.awaitGatewayLoginPauseAfterAgentList();
        } catch (Throwable t) {
            if (isDispatcherPacketApiChainFailure(t)) {
                DISPATCHER_DYNAMIC_PACKET_API_OK.set(Boolean.FALSE);
                logDispatcherPacketApiFirstFailure(dispatcher, t);
                return;
            }
            rethrow(t);
        }
    }

    private void safeDisconnect(IWorkflowContext context) {
        try {
            IDispatcher dispatcher = safeDispatcher(context);
            if (dispatcher != null) {
                dispatcher.disconnect();
                return;
            }
        } catch (Throwable t) {
            log.debug("Dispatcher disconnect failed: {}", t.getMessage());
        }
        try {
            IProxyConnection proxyConnection = resolveWorkflowProxyConnection(context, null);
            if (proxyConnection != null) {
                proxyConnection.disconnect();
            }
        } catch (Throwable t) {
            log.debug("Proxy disconnect failed: {}", t.getMessage());
        }
    }

    private static Charset resolveCharset(String charsetName) {
        String normalized = charsetName != null ? charsetName.trim() : "";
        if (normalized.isEmpty()) {
            return Charset.forName("windows-1252");
        }
        try {
            return Charset.forName(normalized);
        } catch (Exception ignored) {
            return Charset.forName("windows-1252");
        }
    }

    private static long asLong(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static void rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}
