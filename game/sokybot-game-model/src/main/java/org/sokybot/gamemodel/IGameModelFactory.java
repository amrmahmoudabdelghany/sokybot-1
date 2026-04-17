package org.sokybot.gamemodel;

import org.sokybot.gamemodel.spi.IGameModelMutator;

public interface IGameModelFactory {
    IGameModel create(String machineName);

    IGameModelMutator getMutator(IGameModel gameModel);
}
