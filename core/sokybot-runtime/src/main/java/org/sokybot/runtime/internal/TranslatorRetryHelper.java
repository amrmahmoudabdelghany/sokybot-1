package org.sokybot.runtime.internal;

import java.util.List;
import java.util.Map;

import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.slf4j.Logger;

/**
 * Shared translator refresh loops for machine init vs factory assembly.
 */
final class TranslatorRetryHelper {

    private static final int MACHINE_INIT_ATTEMPTS = 12;
    private static final long MACHINE_INIT_DELAY_MS = 400L;
    private static final int FACTORY_ATTEMPTS = 6;
    private static final long FACTORY_DELAY_MS = 250L;

    private TranslatorRetryHelper() {
    }

    static Map<Integer, List<IPacketTranslator>> resolveForMachineInit(ITranslatorRefreshable refreshable,
            Map<Integer, List<IPacketTranslator>> initial,
            Logger log,
            String machineId) {
        Map<Integer, List<IPacketTranslator>> translatorsToWire = initial;
        if (translatorsToWire != null && !translatorsToWire.isEmpty()) {
            return translatorsToWire;
        }
        for (int tAttempt = 0; tAttempt < MACHINE_INIT_ATTEMPTS; tAttempt++) {
            if (tAttempt > 0) {
                sleepQuietly(MACHINE_INIT_DELAY_MS);
                refreshable.invalidateTranslators();
            }
            translatorsToWire = refreshable.getTranslators();
            if (translatorsToWire != null && !translatorsToWire.isEmpty()) {
                log.info("Resolved {} packet translators for machine {} after deferred load (attempt {})",
                        Integer.valueOf(translatorsToWire.size()), machineId, Integer.valueOf(tAttempt + 1));
                break;
            }
        }
        return translatorsToWire != null ? translatorsToWire : Map.of();
    }

    static Map<Integer, List<IPacketTranslator>> resolveForFactory(ITranslatorRefreshable refreshable) {
        Map<Integer, List<IPacketTranslator>> sharedTranslators = Map.of();
        for (int tAttempt = 0; tAttempt < FACTORY_ATTEMPTS; tAttempt++) {
            if (tAttempt > 0) {
                sleepQuietly(FACTORY_DELAY_MS);
                refreshable.invalidateTranslators();
            }
            sharedTranslators = refreshable.getTranslators();
            if (!sharedTranslators.isEmpty()) {
                break;
            }
        }
        return sharedTranslators;
    }

    private static void sleepQuietly(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
