package org.sokybot.engine.internal.cycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.sokybot.engine.api.EngineState;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Engine cycle and coarse state (IDLE / ACTIVE) coordination.
 */
public final class CycleController implements ICycleController {

    private static final Logger log = LoggerFactory.getLogger(CycleController.class);

    private final IWorkflowRegistry workflowRegistry;
    private final AtomicReference<EngineState> state = new AtomicReference<>(EngineState.STOPPED);
    private final AtomicReference<DesiredMode> desiredMode = new AtomicReference<>(DesiredMode.IDLE);

    private enum DesiredMode {
        IDLE,
        TRAINING
    }

    public CycleController(IWorkflowRegistry workflowRegistry) {
        this.workflowRegistry = workflowRegistry;
    }

    @Override
    public void enableCycle(String cycleName) {
        if (workflowRegistry != null) {
            workflowRegistry.setCycleEnabled(cycleName, true);
            log.info("Enabled cycle: {}", cycleName);
        }
    }

    @Override
    public void disableCycle(String cycleName) {
        if (workflowRegistry != null) {
            workflowRegistry.setCycleEnabled(cycleName, false);
            log.info("Disabled cycle: {}", cycleName);
        }
    }

    @Override
    public void reconcileAfterAuthentication() {
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
    public EngineState getEngineState() {
        return state.get();
    }

    @Override
    public boolean compareAndSetState(EngineState expected, EngineState updated) {
        return state.compareAndSet(expected, updated);
    }

    @Override
    public List<String> getActiveActivities() {
        List<String> activities = new ArrayList<>();
        if (workflowRegistry.isCycleEnabled("training-cycle")) {
            activities.add("TRAINING");
        }
        if (workflowRegistry.isCycleEnabled("login-cycle")) {
            activities.add("LOGIN");
        }
        if (activities.isEmpty()) {
            activities.add("IDLE");
        }
        return activities;
    }

    @Override
    public void setDesiredModeTraining() {
        desiredMode.set(DesiredMode.TRAINING);
    }

    @Override
    public void setDesiredModeIdle() {
        desiredMode.set(DesiredMode.IDLE);
    }

    /**
     * Direct state assignment (start/stop lifecycle).
     */
    public void setEngineState(EngineState newState) {
        state.set(newState);
    }
}
