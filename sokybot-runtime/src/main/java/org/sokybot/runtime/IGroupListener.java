package org.sokybot.runtime;



/**
 * Listener interface for group lifecycle events.
 */
public interface IGroupListener {
    
    /**
     * Called when a new group is installed.
     * @param groupContext The newly installed group context
     */
    void onGroupInstalled(IGroupContext groupContext);
}
