package org.sokybot.behaviors.swarm.sentinel;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.sokybot.swarm.api.sentinel.IThreatLedgerCache;

@Component(service = IThreatLedgerCache.class, immediate = true)
public final class ThreatLedgerCacheImpl implements IThreatLedgerCache {

    private final ConcurrentHashMap<String, Long> ledger = new ConcurrentHashMap<>();

    private static String normalize(String characterName) {
        if (characterName == null) {
            return "";
        }
        return characterName.trim().toLowerCase();
    }

    @Override
    public void upsertHostile(String characterName, long expiryEpochMs) {
        String key = normalize(characterName);
        if (key.isEmpty()) {
            return;
        }
        ledger.merge(key, expiryEpochMs, Long::max);
    }

    @Override
    public boolean isHostile(String characterName) {
        String key = normalize(characterName);
        if (key.isEmpty()) {
            return false;
        }
        Long exp = ledger.get(key);
        return exp != null && exp.longValue() > System.currentTimeMillis();
    }

    @Override
    public Set<String> getActiveHostiles(long currentTimeMs) {
        Set<String> active = new HashSet<>();
        for (var e : ledger.entrySet()) {
            Long exp = e.getValue();
            if (exp != null && exp.longValue() > currentTimeMs) {
                active.add(e.getKey());
            }
        }
        return active;
    }
}
