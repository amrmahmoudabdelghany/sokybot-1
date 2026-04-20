package org.sokybot.engine.api;

public interface IEngineControl {
    void setDesiredModeIdle(String machineId);
    void setDesiredModeTraining(String machineId);
    void disableCycle(String machineId, String cycleName);
    void enableCycle(String machineId, String cycleName);
    void disconnectNow(String machineId, String reason);
    void suppressAutoReloginUntil(String machineId, long epochMs);
    long getAutoReloginSuppressedUntilMs(String machineId);
}
