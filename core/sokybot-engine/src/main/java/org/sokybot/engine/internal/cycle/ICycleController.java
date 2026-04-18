package org.sokybot.engine.internal.cycle;

import java.util.List;

import org.sokybot.engine.api.EngineState;

/**
 * Owns engine high-level state, desired training/idle mode, and cycle toggles.
 */
public interface ICycleController {

    void enableCycle(String cycleName);

    void disableCycle(String cycleName);

    void reconcileAfterAuthentication();

    EngineState getEngineState();

    boolean compareAndSetState(EngineState expected, EngineState updated);

    List<String> getActiveActivities();

    void setDesiredModeTraining();

    void setDesiredModeIdle();
}
