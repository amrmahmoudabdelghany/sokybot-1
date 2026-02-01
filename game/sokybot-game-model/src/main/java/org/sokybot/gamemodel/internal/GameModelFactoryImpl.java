package org.sokybot.gamemodel.internal;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.IGameModelFactory;

@Component(service = IGameModelFactory.class)
public class GameModelFactoryImpl implements IGameModelFactory {

    private IReactiveEventBus eventBus;

    @Reference
    public void setEventBus(IReactiveEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public IGameModel create(String machineName) {
        GameModelImpl model = new GameModelImpl(machineName, eventBus);
        model.start(); // Subscribes to bus
        return model;
    }
}
