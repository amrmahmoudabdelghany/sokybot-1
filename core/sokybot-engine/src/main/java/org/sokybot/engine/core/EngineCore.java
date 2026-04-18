package org.sokybot.engine.core;

import org.sokybot.engine.IEngine;
import org.sokybot.engine.api.EngineEvent;
import org.sokybot.engine.api.EngineState;
import org.sokybot.engine.api.event.Wake;
import org.sokybot.engine.api.handler.IEngineEventHandler;
import org.sokybot.engine.api.handler.IEngineEventMediator;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.sokybot.engine.core.dispatcher.DispatcherImpl;
import org.sokybot.engine.core.execution.ParentCycleExecutor;
import org.sokybot.engine.core.interruption.InterruptionManager;
import org.sokybot.engine.core.queue.ActionQueueImpl;
import org.sokybot.engine.core.queue.ActionQueueProcessorImpl;
import org.sokybot.engine.core.waiting.WaitingStateManager;
import org.sokybot.engine.core.workflow.WorkflowContextImpl;
import org.sokybot.engine.core.workflow.WorkflowRegistryImpl;
import org.sokybot.engine.internal.NoopEngineEventMediator;
import org.sokybot.engine.internal.connection.EngineConnectionListener;
import org.sokybot.engine.internal.cycle.CycleController;
import org.sokybot.engine.internal.journal.NetworkTransitionJournal;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.proxy.IProxyConnection;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Core engine implementation.
 * Main entry point for engine functionality.
 */
public class EngineCore implements IEngine {

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
    private final CycleController cycleController;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Object eventLock = new Object();
    private final Map<Class<? extends EngineEvent>, IEngineEventHandler<? extends EngineEvent>> eventHandlers = new ConcurrentHashMap<>();
    private final Map<Class<? extends EngineEvent>, Integer> eventHandlerRankings = new ConcurrentHashMap<>();
    private final EngineRuntimeAdapter runtimeAdapter = new EngineRuntimeAdapter(this);
    private volatile IEngineEventMediator eventMediator = NoopEngineEventMediator.INSTANCE;

    // Actuator management
    private final ActuatorRegistry actuatorRegistry;

    public EngineCore(String machineId, String groupName, String machineName,
            IProxyConnection proxyConnection, IGameModel gameModel,
            java.util.List<org.sokybot.engine.api.extension.IActuator> actuators,
            java.util.List<IEngineEventHandler<? extends EngineEvent>> handlers,
            IEngineEventMediator eventMediator,
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
        this.dispatcher = new DispatcherImpl(proxyConnection, machineId, gameModel);
        this.workflowRegistry = new WorkflowRegistryImpl();
        this.workflowContext = new WorkflowContextImpl(
                gameModel, dispatcher, proxyConnection, groupName, machineName, bundleContext);
        if (this.dispatcher instanceof DispatcherImpl) {
            ((DispatcherImpl) this.dispatcher).bindWorkflowContext(this.workflowContext);
        }
        this.actionQueue = new ActionQueueImpl();
        this.queueProcessor = new ActionQueueProcessorImpl(actionQueue);
        this.interruptionManager = new InterruptionManager(workflowRegistry);

        this.cycleController = new CycleController(workflowRegistry);

        NetworkTransitionJournal networkJournal = new NetworkTransitionJournal(workflowContext);

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
        if (handlers != null) {
            handlers.stream()
                    .sorted((a, b) -> Integer.compare(b.getRanking(), a.getRanking()))
                    .forEach(this::bindEventHandler);
        }
        if (eventMediator != null) {
            this.eventMediator = eventMediator;
        }

        this.proxyConnection.setConnectionListener(new EngineConnectionListener(
                machineId,
                networkJournal,
                cycleController,
                gameModel,
                () -> actuatorRegistry.clearSessionData()));

        log.info("EngineCore created for machine: {}", machineId);
    }

    @Override
    public String getMachineId() {
        return machineId;
    }

    @Override
    public void start() {
        if (!cycleController.compareAndSetState(EngineState.STOPPED, EngineState.IDLE)) {
            EngineState s = cycleController.getEngineState();
            if (s == EngineState.ACTIVE || s == EngineState.IDLE) {
                log.warn("Engine already started for machine: {}", machineId);
                return;
            }
            throw new IllegalStateException("Engine is in invalid state: " + s);
        }

        log.info("Starting engine for machine: {}", machineId);

        try {
            // Initialize actuators
            actuatorRegistry.initializeActuators();

            // Login-cycle is enabled from machine.start via EngineCore.sendEvent("CONNECT").
            // Engines created for resume-on-boot stay idle until the user starts them from the UI.

            // Start parent cycle executor
            parentExecutor.start();

            cycleController.setEngineState(EngineState.IDLE);
            log.info("Engine started successfully for machine: {}", machineId);

        } catch (Exception e) {
            cycleController.setEngineState(EngineState.STOPPED);
            log.error("Failed to start engine for machine {}: {}", machineId, e.getMessage(), e);
            throw new IllegalStateException("Failed to start engine: " + e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        EngineState currentState = cycleController.getEngineState();
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

            cycleController.setEngineState(EngineState.STOPPED);
            log.info("Engine stopped successfully for machine: {}", machineId);

        } catch (Exception e) {
            log.error("Error stopping engine for machine {}: {}", machineId, e.getMessage(), e);
            cycleController.setEngineState(EngineState.STOPPED); // Force to stopped state
        }
    }

    @Override
    public boolean isRunning() {
        EngineState currentState = cycleController.getEngineState();
        return currentState == EngineState.IDLE || currentState == EngineState.ACTIVE;
    }

    @Override
    public EngineState getEngineState() {
        return cycleController.getEngineState();
    }

    @Override
    public void dispatch(EngineEvent event) {
        if (!isRunning()) {
            throw new IllegalStateException("Engine is not running");
        }
        if (event == null || event.type() == null || event.type().trim().isEmpty()) {
            throw new IllegalArgumentException("EngineEvent type cannot be null or empty");
        }
        String eventName = event.type().toUpperCase();
        log.info("Received event: {} for machine: {}", eventName, machineId);

        synchronized (eventLock) {
            IEngineEventHandler<? extends EngineEvent> handler = eventHandlers.get(event.getClass());
            if (handler != null) {
                @SuppressWarnings("unchecked")
                IEngineEventHandler<EngineEvent> typed = (IEngineEventHandler<EngineEvent>) handler;
                typed.handle(event, runtimeAdapter);
                return;
            }

            log.warn("Unknown event: {} for machine: {}", eventName, machineId);
        }
    }

    @Override
    public void sendEvent(EngineEvent event) {
        dispatch(event);
    }

    @Override
    public void wakeWorkflow() {
        dispatch(Wake.INSTANCE);
    }

    void enableCycleInternal(String cycleName) {
        cycleController.enableCycle(cycleName);
    }

    void disableCycleInternal(String cycleName) {
        cycleController.disableCycle(cycleName);
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
        return cycleController.getActiveActivities();
    }

    void publishLifecycleInternal(String eventType) {
        log.debug("Lifecycle event [{}] for machine {}", eventType, machineId);
    }

    void publishStateChangedInternal() {
        log.debug("State changed for machine {} -> {} ({})", machineId, cycleController.getEngineState(),
                getActiveActivities());
    }

    public void bindEventHandler(IEngineEventHandler<? extends EngineEvent> handler) {
        if (handler == null || handler.eventType() == null) {
            return;
        }
        Class<? extends EngineEvent> eventType = handler.eventType();
        Integer existingRanking = eventHandlerRankings.get(eventType);
        if (existingRanking == null || handler.getRanking() >= existingRanking.intValue()) {
            eventHandlers.put(eventType, handler);
            eventHandlerRankings.put(eventType, Integer.valueOf(handler.getRanking()));
        }
    }

    public void unbindEventHandler(IEngineEventHandler<? extends EngineEvent> handler) {
        if (handler == null || handler.eventType() == null) {
            return;
        }
        Class<? extends EngineEvent> eventType = handler.eventType();
        IEngineEventHandler<? extends EngineEvent> current = eventHandlers.get(eventType);
        if (current == handler) {
            eventHandlers.remove(eventType);
            eventHandlerRankings.remove(eventType);
        }
    }

    public void setEventMediator(IEngineEventMediator mediator) {
        this.eventMediator = mediator != null ? mediator : NoopEngineEventMediator.INSTANCE;
    }

    public void clearEventMediator(IEngineEventMediator mediator) {
        if (this.eventMediator == mediator) {
            this.eventMediator = NoopEngineEventMediator.INSTANCE;
        }
    }

    void triggerTransitionInternal() {
        parentExecutor.triggerTransition();
    }

    boolean compareAndSetState(EngineState expected, EngineState updated) {
        return cycleController.compareAndSetState(expected, updated);
    }

    IGameModel gameModel() {
        return gameModel;
    }

    WorkflowContextImpl workflowContext() {
        return workflowContext;
    }

    IEngineEventMediator eventMediator() {
        return eventMediator;
    }

    void setDesiredModeTraining() {
        cycleController.setDesiredModeTraining();
    }

    void setDesiredModeIdle() {
        cycleController.setDesiredModeIdle();
    }

}
