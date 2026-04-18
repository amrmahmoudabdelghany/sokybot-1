package org.sokybot.runtime.internal;

import java.util.List;
import java.util.Map;

import org.sokybot.gameevents.events.core.IPacketTranslator;

/**
 * Group-level translator snapshot and cache invalidation (DIP for machine wiring).
 */
interface ITranslatorRefreshable {

    Map<Integer, List<IPacketTranslator>> getTranslators();

    void invalidateTranslators();
}
