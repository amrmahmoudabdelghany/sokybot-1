package org.sokybot.translators.party;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Placeholder activator for party packet translators ({@code PartyMatchingEvent}, {@code PartyUpdateEvent}, etc.).
 * Translators will be registered here in a later phase.
 */
public final class PartyTranslatorsActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) {
        // Intentionally empty — future home for byte decoders wired to sokybot-game-events-api POJOs.
    }

    @Override
    public void stop(BundleContext context) {
        // Intentionally empty
    }
}
