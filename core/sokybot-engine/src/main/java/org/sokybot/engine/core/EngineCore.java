package org.sokybot.engine.core;

import org.sokybot.engine.IEngine;
import org.sokybot.engine.api.EngineEvent;
import org.sokybot.engine.api.EngineState;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.sokybot.engine.core.dispatcher.DispatcherImpl;
import org.sokybot.engine.core.execution.ParentCycleExecutor;
import org.sokybot.engine.core.interruption.InterruptionManager;
import org.sokybot.engine.core.queue.ActionQueueImpl;
import org.sokybot.engine.core.queue.ActionQueueProcessorImpl;
import org.sokybot.engine.core.waiting.WaitingStateManager;
import org.sokybot.engine.core.workflow.WorkflowContextImpl;
import org.sokybot.engine.core.workflow.WorkflowRegistryImpl;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.LoginState;
import org.sokybot.proxy.IConnectionListener;
import org.sokybot.proxy.IProxyConnection;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Core engine implementation.
 * Main entry point for engine functionality.
 */
public class EngineCore implements IEngine, IConnectionListener {

    private static final Logger log = LoggerFactory.getLogger(EngineCore.class);

    private final String machineId;
    private final String groupName;
    private final String machineName;
    private final IProxyConnection proxyConnection;
    private final IGameModel gameModel;

    // Core components
    private final DispatcherImpl dispatcher;
    private final WorkflowRegistryImpl workflowRegistry;
    private final WorkflowContextImpl workflowContext;
    private final ActionQueueImpl actionQueue;
    private final ActionQueueProcessorImpl queueProcessor;
    private final InterruptionManager interruptionManager;
    private final WaitingStateManager waitingManager;
    private final ParentCycleExecutor parentExecutor;

    // State management
    private final AtomicReference<EngineState> state = new AtomicReference<>(EngineState.STOPPED);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final AtomicReference<DesiredMode> desiredMode = new AtomicReference<>(DesiredMode.IDLE);
    private final Object eventLock = new Object();

    // Actuator management
    private final ActuatorRegistry actuatorRegistry;

    private static final String KEY_NETWORK_TRANSITIONS = "networkTransitions";
    private static final int NETWORK_TRANSITIONS_MAX = 5;

    private enum DesiredMode {
        IDLE,
        TRAINING
    }

    public EngineCore(String machineId, String groupName, String machineName,
            IProxyConnection proxyConnection, IGameModel gameModel,
            java.util.List<org.sokybot.engine.api.extension.IActuator> actuators,
            BundleContext bundleContext) {
        if (machineId == null || machineId.trim().isEmpty()) {
            throw new IllegalArgumentException("Machine ID cannot be null or empty");
        }
        if (proxyConnection == null) {
            throw new IllegalArgumentException("Proxy connection cannot be null");
        }
        if (gameModel == null) {
            throw new IllegalArgumentException("Game model cannot be null");
        }
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        if (machineName == null || machineName.trim().isEmpty()) {
            throw new IllegalArgumentException("Machine name cannot be null or empty");
        }

        this.machineId = machineId;
        this.groupName = groupName;
        this.machineName = machineName;
        this.proxyConnection = proxyConnection;
        this.gameModel = gameModel;

        // Initialize components
        this.dispatcher = new DispatcherImpl(proxyConnection, machineId);
        this.workflowRegistry = new WorkflowRegistryImpl();
        this.workflowContext = new WorkflowContextImpl(
                gameModel, dispatcher, groupName, machineName, bundleContext);
        this.actionQueue = new ActionQueueImpl();
        this.queueProcessor = new ActionQueueProcessorImpl(actionQueue);
        this.interruptionManager = new InterruptionManager(workflowRegistry);

        // Create parent executor (with null waitingManager initially due to circular
        // dependency)
        ParentCycleExecutor tempExecutor = new ParentCycleExecutor(
                workflowRegistry, interruptionManager, queueProcessor,
                null, workflowContext);
        this.parentExecutor = tempExecutor;

        // Create waiting manager with callbacks
        this.waitingManager = new WaitingStateManager(
                scheduler,
                () -> parentExecutor.triggerTransition(),
                () -> {
                    log.info("Stop requested during WAITING state");
                    stop();
                });

        // Set waiting manager in parent executor
        this.parentExecutor.setWaitingManager(waitingManager);

        // Initialize actuator registry (with BundleContext for OSGi service discovery)
        this.actuatorRegistry = new ActuatorRegistry(
                workflowRegistry, workflowContext, this, actuators, bundleContext);

        this.proxyConnection.setConnectionListener(this);

        log.info("EngineCore created for machine: {}", machineId);
    }

    @Override
    public String getMachineId() {
        return machineId;
    }

    @Override
    public void start() {
        if (!state.compareAndSet(EngineState.STOPPED, EngineState.IDLE)) {
            if (state.get() == EngineState.ACTIVE || state.get() == EngineState.IDLE) {
                log.warn("Engine already started for machine: {}", machineId);
                return;
            }
            throw new IllegalStateException("Engine is in invalid state: " + state.get());
        }

        log.info("Starting engine for machine: {}", machineId);

        try {
            // Initialize actuators
            actuatorRegistry.initializeActuators();

            // Login-cycle is enabled from machine.start via EngineCore.sendEvent("CONNECT").
            // Engines created for resume-on-boot stay idle until the user starts them from the UI.

            // Start parent cycle executor
            parentExecutor.start();

            state.set(EngineState.IDLE);
            log.info("Engine started successfully for machine: {}", machineId);

        } catch (Exception e) {
            state.set(EngineState.STOPPED);
            log.error("Failed to start engine for machine {}: {}", machineId, e.getMessage(), e);
            throw new IllegalStateException("Failed to start engine: " + e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        EngineState currentState = state.get();
        if (currentState == EngineState.STOPPED) {
            log.warn("Engine already stopped for machine: {}", machineId);
            return;
        }

        log.info("Stopping engine for machine: {}", machineId);

        try {
            // Stop parent executor
            if (parentExecutor != null) {
                parentExecutor.stop();
            }

            // Shutdown actuators
            if (actuatorRegistry != null) {
                actuatorRegistry.shutdownActuators();
            }

            state.set(EngineState.STOPPED);
            log.info("Engine stopped successfully for machine: {}", machineId);

        } catch (Exception e) {
            log.error("Error stopping engine for machine {}: {}", machineId, e.getMessage(), e);
            state.set(EngineState.STOPPED); // Force to stopped state
        }
    }

    @Override
    public boolean isRunning() {
        EngineState currentState = state.get();
        return currentState == EngineState.IDLE || currentState == EngineState.ACTIVE;
    }

    @Override
    public EngineState getEngineState() {
        return state.get();
    }

    @Override
    public void sendEvent(EngineEvent event) {
        if (!isRunning()) {
            throw new IllegalStateException("Engine is not running");
        }
        if (event == null || event.type() == null || event.type().trim().isEmpty()) {
            throw new IllegalArgumentException("EngineEvent type cannot be null or empty");
        }
        String eventName = event.type().toUpperCase();
        log.info("Received event: {} for machine: {}", eventName, machineId);

        // Handle events
        synchronized (eventLock) {
        switch (eventName) {
            case "START_TRAINING":
                desiredMode.set(DesiredMode.TRAINING);
                // Enable training cycle
                enableCycle("training-cycle");
                state.compareAndSet(EngineState.IDLE, EngineState.ACTIVE);
                publishStateChanged();
                break;

            case "STOP_TRAINING":
                desiredMode.set(DesiredMode.IDLE);
                // Disable training cycle
                disableCycle("training-cycle");
                state.compareAndSet(EngineState.ACTIVE, EngineState.IDLE);
                publishStateChanged();
                break;

            case "CONNECT":
                workflowContext.getPersistentData().put("explicitConnectRequested", true);
                enableCycle("login-cycle");
                publishLifecycle("CONNECT");
                break;

            case "DISCONNECT":
                desiredMode.set(DesiredMode.IDLE);
                workflowContext.getPersistentData().remove("explicitConnectRequested");
                disableCycle("login-cycle");
                disableCycle("training-cycle");
                dispatcher.disconnect();
                gameModel.getLoginState().reset();
                state.compareAndSet(EngineState.ACTIVE, EngineState.IDLE);
                publishLifecycle("DISCONNECT");
                publishStateChanged();
                break;

            default:
                log.warn("Unknown event: {} for machine: {}", eventName, machineId);
        }
        }
    }

    private void enableCycle(String cycleName) {
        if (workflowRegistry != null) {
            workflowRegistry.setCycleEnabled(cycleName, true);
            log.info("Enabled cycle: {}", cycleName);
        }
    }

    private void disableCycle(String cycleName) {
        if (workflowRegistry != null) {
            workflowRegistry.setCycleEnabled(cycleName, false);
            log.info("Disabled cycle: {}", cycleName);
        }
    }

    @Override
    public IWorkflowRegistry getWorkflowRegistry() {
        return workflowRegistry;
    }

    // Package-private getters for actuator registry
    @Override
    public String getGroupName() {
        return groupName;
    }

    @Override
    public String getMachineName() {
        return machineName;
    }

    IGameModel getGameModel() {
        return gameModel;
    }

    @Override
    public DispatcherImpl getDispatcher() {
        return dispatcher;
    }

    /**
     * Gets the actuator registry.
     * Package-private for EngineFactory.
     * 
     * @return The actuator registry
     */
    public ActuatorRegistry getActuatorRegistry() {
        return actuatorRegistry;
    }

    @Override
    public void onClientConnected() {
        log.info("EngineCore: Client connected for machine {}", machineId);
        appendNetworkTransition("ClientConnected", null, null, null);
    }

    @Override
    public void onServerConnected() {
        log.info("EngineCore: Game server connected for machine {}", machineId);
        // Note: The handshake is handled automatically by the Netty pipeline
        // (ClientServerBridge -> HandshakeHandler). No manual action needed here.
        appendNetworkTransition("ServerConnected", null, null, null);
    }

    @Override
    public void onSecuritySetupComplete() {
        log.info("EngineCore: Security setup complete (Blowfish/CRC/Count initialized)");
    }

    @Override
    public void onHandshakeComplete() {
        log.info("EngineCore: Handshake complete for machine {}", machineId);
        appendNetworkTransition("HandshakeComplete", null, null, null);
    }

    @Override
    public void onHandshakeFailed(String reason) {
        log.error("EngineCore: Handshake failed for machine {}: {}", machineId, reason);
        appendNetworkTransition("HandshakeFailed", "HANDSHAKE_FAILED", reason, null);
    }

    @Override
    public void onRedirectRequired(String host, int port, int loginId) {
        log.info("EngineCore: Redirect requested to {}:{} (Login ID: {})", host, port, loginId);
        appendNetworkTransition("RedirectRequired", "REDIRECTING", null, host + ":" + port);
    }

    @Override
    public void onServerIdentified(String serviceName) {
        log.info("EngineCore: Server identified as {} for machine {}", serviceName, machineId);
        appendNetworkTransition("ServerIdentified", serviceName, null, null);
        if ("GatewayServer".equalsIgnoreCase(serviceName)) {
            gameModel.getLoginState().setPhase(LoginState.Phase.GATEWAY_CONNECTED);
        } else if ("AgentServer".equalsIgnoreCase(serviceName)) {
            gameModel.getLoginState().setPhase(LoginState.Phase.AGENT_CONNECTED);
        }
    }

    @Override
    public void onAuthenticated() {
        log.info("EngineCore: Agent authentication complete for machine {}", machineId);
        appendNetworkTransition("Authenticated", "AUTHENTICATED", null, null);
        reconcileDesiredMode();
    }

    private void reconcileDesiredMode() {
        DesiredMode mode = desiredMode.get();
        if (mode == DesiredMode.TRAINING) {
            enableCycle("training-cycle");
            state.compareAndSet(EngineState.IDLE, EngineState.ACTIVE);
            log.info("Reconciled desired mode after authentication: TRAINING");
        } else {
            disableCycle("training-cycle");
            state.compareAndSet(EngineState.ACTIVE, EngineState.IDLE);
            log.info("Reconciled desired mode after authentication: IDLE");
        }
    }

    @Override
    public void onDisconnected(Throwable cause) {
        if (cause != null) {
            log.error("EngineCore: Disconnected with error: {}", cause.getMessage());
        } else {
            log.info("EngineCore: Disconnected from network");
        }
        String reason = cause != null ? cause.getMessage() : null;
        String loginPhase = (reason != null && reason.toLowerCase().contains("agent auth failed")) ? "AUTH_FAILED" : "DISCONNECTED";
        appendNetworkTransition("Disconnected", loginPhase, reason, null);
        actuatorRegistry.clearSessionData();
    }

    @SuppressWarnings("unchecked")
    private void appendNetworkTransition(String transition, String loginPhase, String reason, String endpoint) {
        try {
            var data = workflowContext.getPersistentData();
            synchronized (data) {
                Object existing = data.get(KEY_NETWORK_TRANSITIONS);
                java.util.List<java.util.Map<String, Object>> list;
                if (existing instanceof java.util.List) {
                    list = (java.util.List<java.util.Map<String, Object>>) existing;
                } else {
                    list = new java.util.ArrayList<>();
                }
                java.util.Map<String, Object> ev = new java.util.HashMap<>();
                ev.put("transition", transition);
                if (loginPhase != null) {
                    ev.put("loginPhase", loginPhase);
                }
                if (reason != null) {
                    ev.put("reason", reason);
                }
                if (endpoint != null) {
                    ev.put("endpoint", endpoint);
                }
                ev.put("timestamp", System.currentTimeMillis());
                list.add(ev);
                while (list.size() > NETWORK_TRANSITIONS_MAX) {
                    list.remove(0);
                }
                data.put(KEY_NETWORK_TRANSITIONS, list);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Shuts down the engine and releases all resources.
     * Called when engine is destroyed.
     */
    public void shutdown() {
        stop();

        // Additional cleanup
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        workflowContext.getStateData().clear();
        workflowContext.getPersistentData().entrySet().removeIf(e -> String.valueOf(e.getKey()).startsWith("session."));
        workflowContext.close();
    }

    @Override
    public List<String> getActiveActivities() {
        List<String> activities = new ArrayList<>();
        if (workflowRegistry.isCycleEnabled("training-cycle")) activities.add("TRAINING");
        if (workflowRegistry.isCycleEnabled("login-cycle")) activities.add("LOGIN");
        if (activities.isEmpty()) activities.add("IDLE");
        return activities;
    }

    private void publishLifecycle(String eventType) {
        log.debug("Lifecycle event [{}] for machine {}", eventType, machineId);
    }

    private void publishStateChanged() {
        log.debug("State changed for machine {} -> {} ({})", machineId, state.get(), getActiveActivities());
    }

}
