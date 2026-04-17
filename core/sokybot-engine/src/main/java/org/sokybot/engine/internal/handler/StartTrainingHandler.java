package org.sokybot.engine.internal.handler;

import org.osgi.service.component.annotations.Component;
import org.sokybot.engine.api.EngineState;
import org.sokybot.engine.api.event.StartTraining;
import org.sokybot.engine.api.handler.IEngineEventHandler;
import org.sokybot.engine.api.handler.IEngineRuntime;

@Component(service = IEngineEventHandler.class)
public class StartTrainingHandler implements IEngineEventHandler<StartTraining> {

    @Override
    public Class<StartTraining> eventType() {
        return StartTraining.class;
    }

    @Override
    public void handle(StartTraining event, IEngineRuntime runtime) {
        runtime.setDesiredModeTraining();
        runtime.enableCycle("training-cycle");
        runtime.compareAndSetEngineState(EngineState.IDLE, EngineState.ACTIVE);
        runtime.publishStateChanged();
    }
}
