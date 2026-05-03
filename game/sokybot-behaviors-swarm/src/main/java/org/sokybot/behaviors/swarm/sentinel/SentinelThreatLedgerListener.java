package org.sokybot.behaviors.swarm.sentinel;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmHostilePlayerEvent;
import org.sokybot.swarm.api.sentinel.IThreatLedgerCache;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #20: feeds {@link IThreatLedgerCache} from {@link SwarmHostilePlayerEvent}.
 */
@Component(immediate = true)
public final class SentinelThreatLedgerListener {

    private static final Logger log = LoggerFactory.getLogger(SentinelThreatLedgerListener.class);

    private static final long DEFAULT_THREAT_MEMORY_MS = 300_000L;

    @Reference
    private ISwarmEventBus swarmBus;

    @Reference
    private IThreatLedgerCache threatLedgerCache;

    private volatile Disposable hostileSub;

    @Activate
    void activate() {
        ISwarmEventBus bus = swarmBus;
        IThreatLedgerCache cache = threatLedgerCache;
        if (bus == null || cache == null) {
            log.warn("SentinelThreatLedgerListener: missing ISwarmEventBus or IThreatLedgerCache");
            return;
        }
        hostileSub = bus.observe(SwarmHostilePlayerEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "SentinelThreatLedgerListener stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onHostile);
    }

    @Deactivate
    void deactivate() {
        Disposable d = hostileSub;
        hostileSub = null;
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onHostile(SwarmHostilePlayerEvent event) {
        if (event == null) {
            return;
        }
        IThreatLedgerCache cache = threatLedgerCache;
        if (cache == null) {
            return;
        }
        long expiry = event.getIncidentEpochMs() + DEFAULT_THREAT_MEMORY_MS;
        cache.upsertHostile(event.getAttackerCharacterName(), expiry);
    }
}
