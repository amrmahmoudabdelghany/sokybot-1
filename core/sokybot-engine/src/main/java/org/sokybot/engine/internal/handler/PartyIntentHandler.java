package org.sokybot.engine.internal.handler;

import org.osgi.service.component.annotations.Component;
import org.sokybot.engine.api.event.PartyIntent;
import org.sokybot.engine.api.handler.IEngineEventHandler;
import org.sokybot.engine.api.handler.IEngineRuntime;

@Component(service = IEngineEventHandler.class)
public class PartyIntentHandler implements IEngineEventHandler<PartyIntent> {

    @Override
    public Class<PartyIntent> eventType() {
        return PartyIntent.class;
    }

    @Override
    public void handle(PartyIntent event, IEngineRuntime runtime) {
        runtime.eventMediator().relay(event);
    }
}
