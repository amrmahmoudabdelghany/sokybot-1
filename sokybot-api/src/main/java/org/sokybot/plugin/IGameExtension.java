package org.sokybot.plugin;

import org.sokybot.service.IMachineService;

public interface IGameExtension {

    /**
     * Called when a new machine (bot) is started.
     * Use this method to subscribe to packets or initialize Extension logic.
     * Check machine.getName() or machine.getGroup() to filter targets.
     * 
     * @param machine The machine service instance.
     */
    void onStart(IMachineService machine);
}
