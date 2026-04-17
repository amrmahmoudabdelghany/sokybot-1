package org.sokybot.engine.api.handler;

import org.sokybot.engine.api.EngineState;
import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.sokybot.gamemodel.IGameModel;

/**
 * Narrow runtime facade exposed to engine event handlers.
 */
public interface IEngineRuntime {

    String machineId();

    IWorkflowContext workflowContext();

    IWorkflowRegistry workflowRegistry();

    void enableCycle(String cycleId);

    void disableCycle(String cycleId);

    EngineState engineState();

    boolean compareAndSetEngineState(EngineState expected, EngineState updated);

    IDispatcher dispatcher();

    IGameModel gameModel();

    void publishLifecycle(String eventType);

    void publishStateChanged();

    void triggerTransition();

    IEngineEventMediator eventMediator();

    void setDesiredModeTraining();

    void setDesiredModeIdle();
}
