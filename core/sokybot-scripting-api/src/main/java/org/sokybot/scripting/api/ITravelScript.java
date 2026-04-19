package org.sokybot.scripting.api;

import java.util.List;

/**
 * Parsed travel automation script (immutable).
 */
public interface ITravelScript {

    /** Stable id derived from the file name without extension. */
    String getId();

    /** First {@code # desc:} line in the file, or empty. */
    String getDescription();

    List<TravelCommand> getCommands();

    default int size() {
        return getCommands().size();
    }
}
