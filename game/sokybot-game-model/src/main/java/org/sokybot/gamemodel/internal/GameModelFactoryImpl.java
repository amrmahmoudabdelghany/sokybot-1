package org.sokybot.gamemodel.internal;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.IGameModelFactory;
import org.sokybot.gamemodel.factory.IEntityFactory;
import org.sokybot.gamemodel.spi.IGameModelMutator;

@Component(service = IGameModelFactory.class)
public class GameModelFactoryImpl implements IGameModelFactory {

    private IReactiveEventBus eventBus;
    private IEntityFactory entityFactory;

    @Reference
    public void setEventBus(IReactiveEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Reference
    public void setEntityFactory(IEntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    @Override
    public IGameModel create(String machineName) {
        GameModelImpl model = new GameModelImpl(machineName, eventBus, entityFactory);
        model.start(); // Subscribes to bus
        return model;
    }

    @Override
    public IGameModelMutator getMutator(IGameModel gameModel) {
        if (!(gameModel instanceof IGameModelMutator)) {
            throw new IllegalArgumentException("Game model does not expose mutator contract: " + gameModel);
        }
        return (IGameModelMutator) gameModel;
    }
}
