package org.sokybot.machine.service;

import org.sokybot.machine.model.IGameModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PickingService implements IPickingService {

    @Autowired
    private IGameModel gameModel;

    @Override
    public boolean isItemPickable() {
        // TODO: Implement logic to check for items in range and filter by settings
        return false;
    }

    @Override
    public void pick() {
        // TODO: Implement picking logic
    }
}
