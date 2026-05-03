package org.sokybot.behaviors.swarm.symphony;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmEvent;
import org.sokybot.swarm.api.symphony.ISymphonyComboSignalCache;
import org.sokybot.swarm.api.symphony.SwarmCombatEffectEvent;
import org.sokybot.swarm.api.symphony.SwarmCombatIntentEvent;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #21: JVM-wide last intent/effect per combo target (fed by {@link ISwarmEventBus}).
 */
@Component(service = ISymphonyComboSignalCache.class, immediate = true)
public final class SymphonyComboSignalCacheImpl implements ISymphonyComboSignalCache {

    private static final Logger log = LoggerFactory.getLogger(SymphonyComboSignalCacheImpl.class);

    private final ConcurrentHashMap<String, SwarmEvent> intents = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SwarmEvent> effects = new ConcurrentHashMap<>();

    @Reference
    private ISwarmEventBus swarmEventBus;

    private volatile Disposable intentSub;
    private volatile Disposable effectSub;

    private static String compositeKey(String comboId, int targetRefId) {
        return comboId + "_" + targetRefId;
    }

    @Activate
    void activate() {
        ISwarmEventBus bus = swarmEventBus;
        if (bus == null) {
            log.warn("SymphonyComboSignalCacheImpl: ISwarmEventBus unavailable");
            return;
        }
        intentSub = bus.observe(SwarmCombatIntentEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "SymphonyComboSignalCacheImpl intent stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::recordIntent);
        effectSub = bus.observe(SwarmCombatEffectEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "SymphonyComboSignalCacheImpl effect stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::recordEffect);
    }

    @Deactivate
    void deactivate() {
        dispose(intentSub);
        dispose(effectSub);
        intentSub = null;
        effectSub = null;
        intents.clear();
        effects.clear();
    }

    private static void dispose(Disposable d) {
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    @Override
    public void recordIntent(SwarmCombatIntentEvent event) {
        if (event == null) {
            return;
        }
        intents.put(compositeKey(event.getComboId(), event.getTargetRefId()), event);
    }

    @Override
    public void recordEffect(SwarmCombatEffectEvent event) {
        if (event == null) {
            return;
        }
        effects.put(compositeKey(event.getComboId(), event.getTargetRefId()), event);
    }

    @Override
    public Optional<SwarmCombatIntentEvent> peekLatestIntent(String comboId, int targetRefId) {
        if (comboId == null || comboId.trim().isEmpty()) {
            return Optional.empty();
        }
        SwarmEvent v = intents.get(compositeKey(comboId.trim(), targetRefId));
        if (v instanceof SwarmCombatIntentEvent) {
            return Optional.of((SwarmCombatIntentEvent) v);
        }
        return Optional.empty();
    }

    @Override
    public Optional<SwarmCombatEffectEvent> peekLatestEffect(String comboId, int targetRefId) {
        if (comboId == null || comboId.trim().isEmpty()) {
            return Optional.empty();
        }
        SwarmEvent v = effects.get(compositeKey(comboId.trim(), targetRefId));
        if (v instanceof SwarmCombatEffectEvent) {
            return Optional.of((SwarmCombatEffectEvent) v);
        }
        return Optional.empty();
    }
}
