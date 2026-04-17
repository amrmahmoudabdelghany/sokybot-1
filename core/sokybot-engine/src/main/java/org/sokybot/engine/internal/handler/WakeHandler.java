package org.sokybot.engine.internal.handler;

import org.osgi.service.component.annotations.Component;
import org.sokybot.engine.api.event.Wake;
import org.sokybot.engine.api.handler.IEngineEventHandler;
import org.sokybot.engine.api.handler.IEngineRuntime;

@Component(service = IEngineEventHandler.class)
public class WakeHandler implements IEngineEventHandler<Wake> {

    @Override
    public Class<Wake> eventType() {
        return Wake.class;
    }

    @Override
    public void handle(Wake event, IEngineRuntime runtime) {
        runtime.triggerTransition();
    }
}
