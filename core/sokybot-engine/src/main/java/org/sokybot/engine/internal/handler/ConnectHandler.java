package org.sokybot.engine.internal.handler;

import org.osgi.service.component.annotations.Component;
import org.sokybot.engine.api.event.Connect;
import org.sokybot.engine.api.handler.IEngineEventHandler;
import org.sokybot.engine.api.handler.IEngineRuntime;

@Component(service = IEngineEventHandler.class)
public class ConnectHandler implements IEngineEventHandler<Connect> {

    @Override
    public Class<Connect> eventType() {
        return Connect.class;
    }

    @Override
    public void handle(Connect event, IEngineRuntime runtime) {
        runtime.workflowContext().getPersistentData().put("explicitConnectRequested", true);
        runtime.enableCycle("login-cycle");
        runtime.publishLifecycle("CONNECT");
    }
}
