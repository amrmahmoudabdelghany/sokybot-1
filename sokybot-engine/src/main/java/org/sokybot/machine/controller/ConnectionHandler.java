package org.sokybot.machine.controller;


import org.slf4j.Logger;
import org.sokybot.app.AppConstants;
import org.sokybot.loader.IGameLoaderService;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.StateEntry;
import org.sokybot.machine.workflow.Transition;
import org.sokybot.machine.model.ClientFeed;
import org.sokybot.machine.model.UserAction;
import org.sokybot.settings.BotType;
import org.sokybot.settings.Settings;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.network.packet.ServerOpcode;
import org.sokybot.proxy.IConnectionListener;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.commons.Helper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.EventHeader;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Controller;

/**
 * Handles connection lifecycle using IProxyConnection.
 * Network protocol details are delegated to sokybot-proxy.
 */
@Controller
@WithStateMachine
public class ConnectionHandler implements IConnectionListener {

    @Autowired
    Settings config;

    @Autowired
    ApplicationContext ctx;

    @Autowired
    StateMachine<MachineState, IMachineEvent> machine;

    @Autowired
    Logger log;

    @Autowired
    IProxyConnection proxyConnection;

    @StateEntry(source = {MachineState.STARTING}, target = MachineState.LAUNCHING)
    public void launchingClient(ExtendedState extendedState) {
        log.info("Launching client mode - starting local server on port {}", AppConstants.CLIENT_DEFAULT_PORT);
        
        // Configure proxy for client mode
        proxyConnection.setClientlessMode(false);
        proxyConnection.startLocalServer(AppConstants.CLIENT_DEFAULT_PORT);
        
        // Launch game client
        ctx.getBean(IGameLoaderService.class)
                .findGameLoader(AppConstants.CLIENT_DEFAULT_LOADER)
                .ifPresentOrElse((gl) -> {
                    log.info("Launching game client");
                    int pid = gl.launch(getClientPath());
                    extendedState.getVariables().put(AppConstants.CLIENT_PID, pid);
                    machine.sendEvent(UserAction.CLIENT_ATTACHED);
                }, () -> {
                    log.info("Could not find game loader");
                    machine.sendEvent(UserAction.DISCONNECT);
                });
    }

    @StateEntry(source = {MachineState.STARTING, MachineState.LAUNCHING}, target = MachineState.CONNECTING)
    public void connecting(ExtendedState extendedState) {
        Settings settings = this.ctx.getBean(Settings.class);
        String targetGateway = settings.getTargetGateway();
        int port = this.ctx.getBean(IGameDataLookup.class).getPort();
        
        // Set clientless mode based on config
        proxyConnection.setClientlessMode(config.getBotType() == BotType.CLIENTLESS);
        
        log.info("Connecting to server {}:{}", targetGateway, port);
        proxyConnection.connectToServer(targetGateway, port);
    }

    @StateEntry(target = MachineState.DISCONNECTING)
    public void disconnecting() {
        log.info("Disconnecting from {}", this.config.getTargetGateway());
        proxyConnection.disconnect();
        this.machine.sendEvent(UserAction.KILL_CLIENT);
    }

    @StateEntry(source = MachineState.LOGINING, target = MachineState.REDIRECTING)
    public void redirect(@EventHeader(name = AppConstants.MACHINE_AGENT_HOST, required = true) String agentHost,
                         @EventHeader(name = AppConstants.MACHINE_AGENT_PORT, required = true) short agentPort,
                         @EventHeader(name = AppConstants.MACHINE_LOGIN_ID, required = true) int loginId) {
        log.info("Redirecting to agent server {}:{}", agentHost, agentPort);
        
        if (this.config.getBotType() != BotType.CLIENTLESS) {
            // Start new local server for client redirect
            proxyConnection.startLocalServer(AppConstants.CLIENT_DEFAULT_PORT);
            
            // Inject patch packet to client
            proxyConnection.sendToClient(patchPacket(loginId, "127.0.0.1", AppConstants.CLIENT_DEFAULT_PORT));
        }
        
        // Disconnect from current server and connect to agent
        proxyConnection.disconnect();
        proxyConnection.connectToServer(agentHost, agentPort);
        
        log.info("Agent Address {}:{}", agentHost, agentPort);
    }

    private MutablePacket patchPacket(int loginId, String localHost, int port) {
        int patchLen = 13 + localHost.length();

        return MutablePacket.getBuilder(patchLen, ServerOpcode.LOGIN_RESPONSE)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .put((byte) 0x01) // login result indicator
                .putInt(loginId)
                .putShort((short) localHost.length())
                .putBytes(localHost.getBytes())
                .putInt(port)
                .putShort((short) 0x00)
                .build();
    }

    @Transition(source = MachineState.WITH_CLIENT, target = MachineState.WITHOUT_CLIENT)
    public void disconnectClient(ExtendedState extendedState) {
        Integer cpid = extendedState.get(AppConstants.CLIENT_PID, Integer.class);
        if (cpid != null && cpid != -1) {
            Helper.killProcess(cpid.intValue());
            extendedState.getVariables().put(AppConstants.CLIENT_PID, -1);
        }
    }

    private String getClientPath() {
        String clientPath = ctx.getBean(IGameDataLookup.class).getGamePath() + "\\sro_client.exe";
        return clientPath;
    }

    // ========== IConnectionListener Implementation ==========

    @Override
    public void onClientConnected() {
        log.info("Client connected to proxy");
        machine.sendEvent(ClientFeed.CLIENT_CONNECTED);
    }

    @Override
    public void onServerConnected() {
        log.info("Proxy connected to game server");
    }

    @Override
    public void onSecuritySetupComplete() {
        log.info("Security protocol configured");
        machine.sendEvent(org.sokybot.machine.model.ServerFeed.SETUP);
    }

    @Override
    public void onHandshakeComplete() {
        log.info("Handshake completed successfully");
        // Advancing state from HANDSHAKING -> CHALLENGING -> IDENTIFYING
        machine.sendEvent(org.sokybot.machine.model.ServerFeed.CHALLENGE);
        machine.sendEvent(ClientFeed.CONNECTION_ACCEPTED);
    }

    @Override
    public void onHandshakeFailed(String reason) {
        log.error("Handshake failed: {}", reason);
        machine.sendEvent(ClientFeed.CONNECTION_REFUSED);
    }

    @Override
    public void onRedirectRequired(String host, int port, int loginId) {
        log.info("Redirect requested to {}:{} with loginId {}", host, port, loginId);
        // The redirect will be handled by state machine via events
    }
    
    @Override
    public void onServerIdentified(String serviceName) {
        log.info("Server identified as: {}", serviceName);
        if ("GatewayServer".equals(serviceName)) {
            machine.sendEvent(org.sokybot.machine.model.ServerFeed.GATEWAY_CONNECTED);
        } else if ("AgentServer".equals(serviceName)) {
            machine.sendEvent(org.sokybot.machine.model.ServerFeed.AGENT_CONNECTED);
        } else {
            machine.sendEvent(org.sokybot.machine.model.ServerFeed.UNKNOWN);
        }
    }

    @Override
    public void onDisconnected(Throwable cause) {
        log.info("Disconnected: {}", cause != null ? cause.getMessage() : "Normal");
    }
}
