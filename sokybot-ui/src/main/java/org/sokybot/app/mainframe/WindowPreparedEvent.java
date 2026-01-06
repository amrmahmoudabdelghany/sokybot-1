package org.sokybot.app.mainframe;

import java.util.EventObject;

/**
 * Event published when the Main Window is fully initialized and visible.
 * This event serves as a signal for other UI components to start their initialization
 * that might depend on the Main Frame being ready.
 */
public class WindowPreparedEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    public WindowPreparedEvent(Object source) {
        super(source);
    }
}
