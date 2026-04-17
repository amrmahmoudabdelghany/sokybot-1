package org.sokybot.behaviors.training.internal;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.behaviors.training.api.TrainingSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;

@Component(service = IBehavior.class, immediate = true)
public class TownBehavior implements IBehavior<TrainingSettings> {
    private static final Logger log = LoggerFactory.getLogger(TownBehavior.class);

    @Override
    public String id() {
        return "town";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return "training-cycle".equals(cycleId);
    }

    @Override
    public Class<TrainingSettings> settingsType() {
        return TrainingSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, TrainingSettings settings) {
        return shouldReturnToTown(context, settings);
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, TrainingSettings settings) {
        log.info("Returning to town logic");
        return BehaviorStatus.SKIPPED;
    }

    @Override
    public long postDelayMs() {
        return 2000L;
    }

    private boolean shouldReturnToTown(IWorkflowContext context, TrainingSettings settings) {
        return settings != null && settings.isLoopInTown() && settings.getScriptPath() != null
                && !settings.getScriptPath().trim().isEmpty();
    }
}
