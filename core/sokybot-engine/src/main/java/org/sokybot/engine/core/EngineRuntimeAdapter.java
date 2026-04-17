package org.sokybot.engine.core;

import org.sokybot.engine.api.EngineState;
import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.handler.IEngineEventMediator;
import org.sokybot.engine.api.handler.IEngineRuntime;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.sokybot.gamemodel.IGameModel;

final class EngineRuntimeAdapter implements IEngineRuntime {

    private final EngineCore engineCore;

    EngineRuntimeAdapter(EngineCore engineCore) {
        this.engineCore = engineCore;
    }

    @Override
    public String machineId() {
        return engineCore.getMachineId();
    }

    @Override
    public IWorkflowContext workflowContext() {
        return engineCore.workflowContext();
    }

    @Override
    public IWorkflowRegistry workflowRegistry() {
        return engineCore.getWorkflowRegistry();
    }

    @Override
    public void enableCycle(String cycleId) {
        engineCore.enableCycleInternal(cycleId);
    }

    @Override
    public void disableCycle(String cycleId) {
        engineCore.disableCycleInternal(cycleId);
    }

    @Override
    public EngineState engineState() {
        return engineCore.getEngineState();
    }

    @Override
    public boolean compareAndSetEngineState(EngineState expected, EngineState updated) {
        return engineCore.compareAndSetState(expected, updated);
    }

    @Override
    public IDispatcher dispatcher() {
        return engineCore.getDispatcher();
    }

    @Override
    public IGameModel gameModel() {
        return engineCore.gameModel();
    }

    @Override
    public void publishLifecycle(String eventType) {
        engineCore.publishLifecycleInternal(eventType);
    }

    @Override
    public void publishStateChanged() {
        engineCore.publishStateChangedInternal();
    }

    @Override
    public void triggerTransition() {
        engineCore.triggerTransitionInternal();
    }

    @Override
    public IEngineEventMediator eventMediator() {
        return engineCore.eventMediator();
    }

    @Override
    public void setDesiredModeTraining() {
        engineCore.setDesiredModeTraining();
    }

    @Override
    public void setDesiredModeIdle() {
        engineCore.setDesiredModeIdle();
    }
}
