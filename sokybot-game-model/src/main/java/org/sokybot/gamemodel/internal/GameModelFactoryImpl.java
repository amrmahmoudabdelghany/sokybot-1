package org.sokybot.gamemodel.internal;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.IGameModelFactory;

@Component(service = IGameModelFactory.class)
public class GameModelFactoryImpl implements IGameModelFactory {

    private BundleContext bundleContext;
    
    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public IGameModel create(String machineName) {
        GameModelImpl model = new GameModelImpl(machineName, bundleContext);
        model.start(); // Registers event handler
        return model;
    }
}
