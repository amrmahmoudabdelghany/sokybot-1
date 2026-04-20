package org.sokybot.behaviors.social;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.behaviors.social.settings.SocialSettings;
import org.sokybot.engine.api.IEngineControl;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.social.api.ISocialModel;
import org.sokybot.social.api.ISocialSnapshot;

import lombok.extern.slf4j.Slf4j;

@Component(service = IBehavior.class, property = { "order=1" }, scope = org.osgi.service.component.annotations.ServiceScope.PROTOTYPE)
@Slf4j
public class GmEvadeBehavior implements IBehavior<SocialSettings> {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISocialModel socialModel;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private IEngineControl engineControl;

    private boolean isEvading = false;

    @Override
    public String id() {
        return "social-gm-evade";
    }

    @Override
    public Class<SocialSettings> settingsType() {
        return SocialSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, SocialSettings settings) {
        if (!settings.isGmEvasionEnabled() || socialModel == null) {
            return false;
        }

        return socialModel.snapshot(context.getMachineId())
                .map(snap -> snap.getChat().isGmActive())
                .orElse(false);
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, SocialSettings settings) {
        if (socialModel == null) return BehaviorStatus.SKIPPED;

        ISocialSnapshot snap = socialModel.snapshot(context.getMachineId()).orElse(null);
        if (snap != null && snap.getChat().isGmActive()) {
            
            if (!isEvading) {
                log.warn("[{}] GM detected! Initiating evasion maneuvers.", context.getMachineId());
                engineControl.setDesiredModeIdle(context.getMachineId());
                isEvading = true;
            }

            if (settings.isDisconnectOnGm()) {
                log.warn("[{}] Disconnecting due to GM presence.", context.getMachineId());
                engineControl.suppressAutoReloginUntil(context.getMachineId(), System.currentTimeMillis() + settings.getGmReloginCooldownMs());
                engineControl.disconnectNow(context.getMachineId(), "GM Evasion");
            }
        }

        return BehaviorStatus.EXECUTED;
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public int interruptionPriority() {
        return 100; // Very high priority, interrupt grinding/walking immediately
    }
}
