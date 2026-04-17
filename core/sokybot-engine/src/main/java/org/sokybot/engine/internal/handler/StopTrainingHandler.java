package org.sokybot.engine.internal.handler;

import org.osgi.service.component.annotations.Component;
import org.sokybot.engine.api.EngineState;
import org.sokybot.engine.api.event.StopTraining;
import org.sokybot.engine.api.handler.IEngineEventHandler;
import org.sokybot.engine.api.handler.IEngineRuntime;

@Component(service = IEngineEventHandler.class)
public class StopTrainingHandler implements IEngineEventHandler<StopTraining> {

    @Override
    public Class<StopTraining> eventType() {
        return StopTraining.class;
    }

    @Override
    public void handle(StopTraining event, IEngineRuntime runtime) {
        runtime.setDesiredModeIdle();
        runtime.disableCycle("training-cycle");
        runtime.compareAndSetEngineState(EngineState.ACTIVE, EngineState.IDLE);
        runtime.publishStateChanged();
    }
}
