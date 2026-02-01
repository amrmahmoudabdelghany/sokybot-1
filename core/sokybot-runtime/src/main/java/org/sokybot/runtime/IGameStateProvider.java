package org.sokybot.runtime;

import org.sokybot.gamemodel.IGameModel;

/**
 * Interface for components that provide access to the game state.
 */
public interface IGameStateProvider {

    /**
     * Gets the game model.
     * 
     * @return The game model instance.
     */
    IGameModel getGameModel();

    /**
     * Gets the group name associated with this state.
     * 
     * @return The group name.
     */
    String getGroupName();

    /**
     * Gets the machine name associated with this state.
     * 
     * @return The machine name.
     */
    String getMachineName();
}
