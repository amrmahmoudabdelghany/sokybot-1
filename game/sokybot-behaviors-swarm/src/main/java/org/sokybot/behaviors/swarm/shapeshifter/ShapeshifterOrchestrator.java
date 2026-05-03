package org.sokybot.behaviors.swarm.shapeshifter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.behaviors.party.internal.settings.shapeshifter.FallbackRule;
import org.sokybot.behaviors.party.internal.settings.shapeshifter.ShapeshifterSettings;
import org.sokybot.combat.api.shapeshifter.IWeaponFlexService;
import org.sokybot.engine.IEngine;
import org.sokybot.engine.api.IEngineControl;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.party.api.shapeshifter.SwarmTacticalRole;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.shapeshifter.ShapeshifterBlackboardKeys;
import org.sokybot.swarm.api.shapeshifter.SwarmRoleDeficitEvent;
import org.sokybot.swarm.api.shapeshifter.SwarmRoleRestoredEvent;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #22 Phase 4: reacts to role deficit/restored swarm events — weapon flex and cycle swap on backup bots.
 */
@Component(immediate = true)
public final class ShapeshifterOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ShapeshifterOrchestrator.class);

    @Reference
    private ISwarmEventBus swarmEventBus;

    @Reference
    private ISokybotContext sokybotContext;

    @Reference
    private ISettingsRegistry settingsRegistry;

    @Reference
    private IEngineControl engineControl;

    @Reference
    private IWeaponFlexService weaponFlexService;

    private volatile Disposable deficitSub;
    private volatile Disposable restoredSub;

    @Activate
    void activate() {
        ISwarmEventBus bus = swarmEventBus;
        if (bus == null) {
            log.warn("ShapeshifterOrchestrator: ISwarmEventBus unavailable");
            return;
        }
        deficitSub = bus.observe(SwarmRoleDeficitEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "ShapeshifterOrchestrator deficit stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onDeficit);
        restoredSub = bus.observe(SwarmRoleRestoredEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "ShapeshifterOrchestrator restored stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onRestored);
    }

    @Deactivate
    void deactivate() {
        dispose(deficitSub);
        dispose(restoredSub);
        deficitSub = null;
        restoredSub = null;
    }

    private static void dispose(Disposable d) {
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onDeficit(SwarmRoleDeficitEvent event) {
        if (event == null) {
            return;
        }
        ISokybotContext ctx = sokybotContext;
        ISettingsRegistry registry = settingsRegistry;
        IWeaponFlexService flex = weaponFlexService;
        IEngineControl control = engineControl;
        if (ctx == null || registry == null || flex == null || control == null) {
            return;
        }
        String swarmGroupId = event.getSwarmGroupId();
        SwarmTacticalRole missingRole = event.getMissingRole();

        for (IGroupContext group : ctx.getGroups()) {
            if (group == null || !Objects.equals(group.name().trim(), swarmGroupId)) {
                continue;
            }
            for (IMachineContext machine : group.getMachines()) {
                if (machine == null || !machine.isRunning()) {
                    continue;
                }
                try {
                    FallbackRule rule = findRuleForBackup(registry, machine, missingRole);
                    if (rule == null) {
                        continue;
                    }
                    applyDeficit(event, machine, rule, flex, control);
                    return;
                } catch (Exception ex) {
                    log.trace("ShapeshifterOrchestrator deficit {}: {}", machine.fullName(), ex.getMessage());
                }
            }
        }
    }

    private void applyDeficit(
            SwarmRoleDeficitEvent event,
            IMachineContext machine,
            FallbackRule rule,
            IWeaponFlexService flex,
            IEngineControl control) {
        IEngine engine = machine.getEngine();
        if (engine == null) {
            return;
        }
        Optional<IWorkflowContext> wfOpt = safeWorkflow(engine);
        if (!wfOpt.isPresent()) {
            return;
        }
        Map<String, Object> pd = wfOpt.get().getPersistentData();
        if (pd == null) {
            return;
        }
        if (Boolean.TRUE.equals(pd.get(ShapeshifterBlackboardKeys.KEY_SHAPESHIFTER_FLEX_ACTIVE))) {
            return;
        }

        String baselineCycle = rule.getBaselineCycleId();
        String flexCycle = rule.getFlexCycleId();
        if (baselineCycle == null || baselineCycle.trim().isEmpty()
                || flexCycle == null || flexCycle.trim().isEmpty()) {
            return;
        }

        int currentWeapon = flex.getCurrentWeaponRefId(machine.fullName());
        pd.put(ShapeshifterBlackboardKeys.KEY_SHAPESHIFTER_BASELINE_WEAPON_REF, Integer.valueOf(currentWeapon));
        pd.put(ShapeshifterBlackboardKeys.KEY_SHAPESHIFTER_BASELINE_CYCLE_ID, baselineCycle.trim());

        flex.equipWeaponByRefId(machine.fullName(), rule.getWeaponItemRefId());

        String mid = machine.fullName();
        control.disableCycle(mid, baselineCycle.trim());
        control.enableCycle(mid, flexCycle.trim());

        pd.put(ShapeshifterBlackboardKeys.KEY_SHAPESHIFTER_FLEX_ACTIVE, Boolean.TRUE);
        pd.put(ShapeshifterBlackboardKeys.KEY_SHAPESHIFTER_ACTIVE_ROLE, event.getMissingRole().name());
    }

    private void onRestored(SwarmRoleRestoredEvent event) {
        if (event == null) {
            return;
        }
        ISokybotContext ctx = sokybotContext;
        ISettingsRegistry registry = settingsRegistry;
        IWeaponFlexService flex = weaponFlexService;
        IEngineControl control = engineControl;
        if (ctx == null || registry == null || flex == null || control == null) {
            return;
        }
        String swarmGroupId = event.getSwarmGroupId();
        SwarmTacticalRole restoredRole = event.getRestoredRole();

        for (IGroupContext group : ctx.getGroups()) {
            if (group == null || !Objects.equals(group.name().trim(), swarmGroupId)) {
                continue;
            }
            for (IMachineContext machine : group.getMachines()) {
                if (machine == null || !machine.isRunning()) {
                    continue;
                }
                try {
                    FallbackRule rule = findRuleForBackup(registry, machine, restoredRole);
                    if (rule == null) {
                        continue;
                    }
                    applyRestored(machine, rule, restoredRole, flex, control);
                    return;
                } catch (Exception ex) {
                    log.trace("ShapeshifterOrchestrator restored {}: {}", machine.fullName(), ex.getMessage());
                }
            }
        }
    }

    private void applyRestored(
            IMachineContext machine,
            FallbackRule rule,
            SwarmTacticalRole restoredRole,
            IWeaponFlexService flex,
            IEngineControl control) {
        IEngine engine = machine.getEngine();
        if (engine == null) {
            return;
        }
        Optional<IWorkflowContext> wfOpt = safeWorkflow(engine);
        if (!wfOpt.isPresent()) {
            return;
        }
        Map<String, Object> pd = wfOpt.get().getPersistentData();
        if (pd == null) {
            return;
        }
        if (!Boolean.TRUE.equals(pd.get(ShapeshifterBlackboardKeys.KEY_SHAPESHIFTER_FLEX_ACTIVE))) {
            return;
        }
        Object rawRole = pd.get(ShapeshifterBlackboardKeys.KEY_SHAPESHIFTER_ACTIVE_ROLE);
        if (!(rawRole instanceof String)) {
            return;
        }
        if (!restoredRole.name().equals(((String) rawRole).trim())) {
            return;
        }

        String flexCycle = rule.getFlexCycleId();
        Object rawBaselineCycle = pd.get(ShapeshifterBlackboardKeys.KEY_SHAPESHIFTER_BASELINE_CYCLE_ID);
        Object rawBaselineWeapon = pd.get(ShapeshifterBlackboardKeys.KEY_SHAPESHIFTER_BASELINE_WEAPON_REF);

        int baselineWeaponRef = 0;
        if (rawBaselineWeapon instanceof Number) {
            baselineWeaponRef = ((Number) rawBaselineWeapon).intValue();
        }

        if (baselineWeaponRef > 0) {
            flex.equipWeaponByRefId(machine.fullName(), baselineWeaponRef);
        }

        String mid = machine.fullName();
        if (flexCycle != null && !flexCycle.trim().isEmpty()) {
            control.disableCycle(mid, flexCycle.trim());
        }
        if (rawBaselineCycle instanceof String) {
            String b = ((String) rawBaselineCycle).trim();
            if (!b.isEmpty()) {
                control.enableCycle(mid, b);
            }
        }

        pd.remove(ShapeshifterBlackboardKeys.KEY_SHAPESHIFTER_FLEX_ACTIVE);
        pd.remove(ShapeshifterBlackboardKeys.KEY_SHAPESHIFTER_ACTIVE_ROLE);
        pd.remove(ShapeshifterBlackboardKeys.KEY_SHAPESHIFTER_BASELINE_CYCLE_ID);
        pd.remove(ShapeshifterBlackboardKeys.KEY_SHAPESHIFTER_BASELINE_WEAPON_REF);
    }

    private static FallbackRule findRuleForBackup(
            ISettingsRegistry registry,
            IMachineContext machine,
            SwarmTacticalRole role) {
        ShapeshifterSettings settings = readShapeshifter(registry, machine);
        if (settings == null || !settings.isShapeshifterEnabled()) {
            return null;
        }
        List<FallbackRule> rules = settings.getRules();
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        String machineName = machine.getMachineName();
        if (machineName == null) {
            return null;
        }
        String mn = machineName.trim();
        for (FallbackRule r : rules) {
            if (r == null || r.getDeficitRole() != role) {
                continue;
            }
            String backup = r.getBackupMachineName();
            if (backup != null && backup.trim().equals(mn)) {
                return r;
            }
        }
        return null;
    }

    private static ShapeshifterSettings readShapeshifter(ISettingsRegistry registry, IMachineContext machine) {
        try {
            ISettingsProvider<ShapeshifterSettings> p = registry.getProvider(
                    machine.getGroupName(),
                    machine.getMachineName(),
                    "shapeshifter",
                    ShapeshifterSettings.class);
            return p != null ? p.get() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static Optional<IWorkflowContext> safeWorkflow(IEngine engine) {
        if (engine == null) {
            return Optional.empty();
        }
        try {
            return engine.optionalWorkflowContext();
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}
