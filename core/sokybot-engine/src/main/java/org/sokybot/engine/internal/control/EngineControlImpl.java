package org.sokybot.engine.internal.control;

import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.IEngine;
import org.sokybot.engine.IEngineFactory;
import org.sokybot.engine.api.IEngineControl;
import org.sokybot.engine.core.EngineCore;

import lombok.extern.slf4j.Slf4j;

@Component(service = IEngineControl.class, immediate = true)
@Slf4j
public class EngineControlImpl implements IEngineControl {

    @Reference
    private IEngineFactory engineFactory;

    private final ConcurrentHashMap<String, Long> reloginSuppressed = new ConcurrentHashMap<>();

    private EngineCore getEngineCore(String machineId) {
        IEngine engine = engineFactory.getEngine(machineId);
        if (engine instanceof EngineCore) {
            return (EngineCore) engine;
        }
        return null;
    }

    @Override
    public void setDesiredModeIdle(String machineId) {
        EngineCore core = getEngineCore(machineId);
        if (core != null) {
            core.getCycleController().setDesiredModeIdle();
            core.getCycleController().reconcileAfterAuthentication();
            log.info("Desired mode set to IDLE for {}", machineId);
        }
    }

    @Override
    public void setDesiredModeTraining(String machineId) {
        EngineCore core = getEngineCore(machineId);
        if (core != null) {
            core.getCycleController().setDesiredModeTraining();
            core.getCycleController().reconcileAfterAuthentication();
            log.info("Desired mode set to TRAINING for {}", machineId);
        }
    }

    @Override
    public void disableCycle(String machineId, String cycleName) {
        EngineCore core = getEngineCore(machineId);
        if (core != null) {
            core.getCycleController().disableCycle(cycleName);
        }
    }

    @Override
    public void enableCycle(String machineId, String cycleName) {
        EngineCore core = getEngineCore(machineId);
        if (core != null) {
            core.getCycleController().enableCycle(cycleName);
        }
    }

    @Override
    public void disconnectNow(String machineId, String reason) {
        EngineCore core = getEngineCore(machineId);
        if (core != null) {
            log.info("Forcing disconnect for {}, reason: {}", machineId, reason);
            core.getProxyConnection().disconnect();
        }
    }

    @Override
    public void suppressAutoReloginUntil(String machineId, long epochMs) {
        reloginSuppressed.put(machineId, epochMs);
        log.info("Auto-relogin suppressed until {} for {}", epochMs, machineId);
    }

    @Override
    public long getAutoReloginSuppressedUntilMs(String machineId) {
        return reloginSuppressed.getOrDefault(machineId, 0L);
    }
}
