package org.sokybot.town.api;

import java.util.Optional;

/**
 * Contributes zero or one active intent for a machine (death projection, town model, manual scripts, …).
 */
public interface IIntentSource {

    /**
     * @param machineFullName stable machine identity (same convention as workflow context)
     */
    Optional<IIntent> currentIntent(String machineFullName);
}
