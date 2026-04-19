package org.sokybot.town.projections.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.town.api.IIntent;
import org.sokybot.town.api.IIntentArbiter;
import org.sokybot.town.api.IIntentSource;
import org.sokybot.town.api.IntentKind;

/**
 * Collapses all {@link IIntentSource} bindings into a priority-ordered domain decision.
 * {@link #release(String, IntentKind)} briefly suppresses {@link IntentKind#TOWN} so hand-off can yield to combat.
 */
@Component(service = IIntentArbiter.class, immediate = true)
public final class DefaultIntentArbiter implements IIntentArbiter {

    private final List<IIntentSource> sources = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, Long> suppressTownUntilEpochMs = new ConcurrentHashMap<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindIntentSource(IIntentSource source) {
        sources.add(source);
    }

    protected void unbindIntentSource(IIntentSource source) {
        sources.remove(source);
    }

    @Override
    public IntentKind decide(String machineFullName) {
        if (machineFullName == null || machineFullName.trim().isEmpty()) {
            return IntentKind.COMBAT;
        }
        List<IIntent> candidates = new ArrayList<>();
        for (IIntentSource src : sources) {
            Optional<IIntent> opt;
            try {
                opt = src.currentIntent(machineFullName);
            } catch (RuntimeException ex) {
                continue;
            }
            opt.ifPresent(candidates::add);
        }
        candidates.sort(Comparator.comparingInt((IIntent i) -> priority(i.getKind())).reversed());
        for (IIntent cand : candidates) {
            if (cand.getKind() == IntentKind.TOWN && isTownSuppressed(machineFullName)) {
                continue;
            }
            return cand.getKind();
        }
        return IntentKind.COMBAT;
    }

    @Override
    public boolean isActive(String machineFullName, IntentKind kind) {
        if (machineFullName == null || kind == null) {
            return false;
        }
        return decide(machineFullName) == kind;
    }

    @Override
    public void release(String machineFullName, IntentKind kind) {
        if (machineFullName == null || kind == null) {
            return;
        }
        if (kind == IntentKind.TOWN) {
            suppressTownUntilEpochMs.put(machineFullName, System.currentTimeMillis() + 3_000L);
        }
    }

    private boolean isTownSuppressed(String machineFullName) {
        Long until = suppressTownUntilEpochMs.get(machineFullName);
        return until != null && System.currentTimeMillis() < until.longValue();
    }

    private static int priority(IntentKind k) {
        switch (k) {
            case DEATH:
                return 4;
            case TOWN:
                return 3;
            case COMBAT:
                return 2;
            case IDLE:
                return 1;
            default:
                return 0;
        }
    }
}
