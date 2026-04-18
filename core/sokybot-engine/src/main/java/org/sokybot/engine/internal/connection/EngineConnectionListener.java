package org.sokybot.engine.internal.connection;

import org.sokybot.engine.internal.cycle.ICycleController;
import org.sokybot.engine.internal.journal.INetworkTransitionJournal;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.LoginState;
import org.sokybot.proxy.IConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges proxy connection events into the network journal, cycle reconciliation,
 * and session cleanup.
 */
public final class EngineConnectionListener implements IConnectionListener {

    private static final Logger log = LoggerFactory.getLogger(EngineConnectionListener.class);

    private final String machineId;
    private final INetworkTransitionJournal journal;
    private final ICycleController cycleController;
    private final IGameModel gameModel;
    private final Runnable clearSessionData;

    public EngineConnectionListener(String machineId,
            INetworkTransitionJournal journal,
            ICycleController cycleController,
            IGameModel gameModel,
            Runnable clearSessionData) {
        this.machineId = machineId;
        this.journal = journal;
        this.cycleController = cycleController;
        this.gameModel = gameModel;
        this.clearSessionData = clearSessionData;
    }

    @Override
    public void onClientConnected() {
        log.info("EngineConnectionListener: Client connected for machine {}", machineId);
        journal.append("ClientConnected", null, null, null);
    }

    @Override
    public void onServerConnected() {
        log.info("EngineConnectionListener: Game server connected for machine {}", machineId);
        journal.append("ServerConnected", null, null, null);
    }

    @Override
    public void onSecuritySetupComplete() {
        log.info("EngineConnectionListener: Security setup complete (Blowfish/CRC/Count initialized)");
    }

    @Override
    public void onHandshakeComplete() {
        log.info("EngineConnectionListener: Handshake complete for machine {}", machineId);
        journal.append("HandshakeComplete", null, null, null);
    }

    @Override
    public void onHandshakeFailed(String reason) {
        log.error("EngineConnectionListener: Handshake failed for machine {}: {}", machineId, reason);
        journal.append("HandshakeFailed", "HANDSHAKE_FAILED", reason, null);
    }

    @Override
    public void onRedirectRequired(String host, int port, int loginId) {
        log.info("EngineConnectionListener: Redirect requested to {}:{} (Login ID: {})", host, port, loginId);
        journal.append("RedirectRequired", "REDIRECTING", null, host + ":" + port);
    }

    @Override
    public void onServerIdentified(String serviceName) {
        log.info("EngineConnectionListener: Server identified as {} for machine {}", serviceName, machineId);
        journal.append("ServerIdentified", serviceName, null, null);
        if ("GatewayServer".equalsIgnoreCase(serviceName)) {
            gameModel.getLoginState().setPhase(LoginState.Phase.GATEWAY_CONNECTED);
        } else if ("AgentServer".equalsIgnoreCase(serviceName)) {
            gameModel.getLoginState().setPhase(LoginState.Phase.AGENT_CONNECTED);
        }
    }

    @Override
    public void onAuthenticated() {
        log.info("EngineConnectionListener: Agent authentication complete for machine {}", machineId);
        journal.append("Authenticated", "AUTHENTICATED", null, null);
        cycleController.reconcileAfterAuthentication();
    }

    @Override
    public void onDisconnected(Throwable cause) {
        if (cause != null) {
            log.error("EngineConnectionListener: Disconnected with error: {}", cause.getMessage());
        } else {
            log.info("EngineConnectionListener: Disconnected from network");
        }
        String reason = cause != null ? cause.getMessage() : null;
        String loginPhase = (reason != null && reason.toLowerCase().contains("agent auth failed")) ? "AUTH_FAILED"
                : "DISCONNECTED";
        journal.append("Disconnected", loginPhase, reason, null);
        clearSessionData.run();
    }
}
