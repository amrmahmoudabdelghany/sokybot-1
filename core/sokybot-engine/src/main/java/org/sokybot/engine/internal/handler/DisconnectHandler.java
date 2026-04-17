package org.sokybot.engine.internal.handler;

import org.osgi.service.component.annotations.Component;
import org.sokybot.engine.api.EngineState;
import org.sokybot.engine.api.event.Disconnect;
import org.sokybot.engine.api.handler.IEngineEventHandler;
import org.sokybot.engine.api.handler.IEngineRuntime;

@Component(service = IEngineEventHandler.class)
public class DisconnectHandler implements IEngineEventHandler<Disconnect> {

    @Override
    public Class<Disconnect> eventType() {
        return Disconnect.class;
    }

    @Override
    public void handle(Disconnect event, IEngineRuntime runtime) {
        runtime.setDesiredModeIdle();
        runtime.workflowContext().getPersistentData().remove("explicitConnectRequested");
        runtime.disableCycle("login-cycle");
        runtime.disableCycle("training-cycle");
        runtime.dispatcher().disconnect();
        runtime.gameModel().getLoginState().reset();
        runtime.compareAndSetEngineState(EngineState.ACTIVE, EngineState.IDLE);
        runtime.publishLifecycle("DISCONNECT");
        runtime.publishStateChanged();
    }
}
