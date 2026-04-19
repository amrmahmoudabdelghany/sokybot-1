package org.sokybot.pet.projections.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.events.character.CharacterDeathEvent;
import org.sokybot.gameevents.events.character.CharacterLoadedEvent;
import org.sokybot.gameevents.events.cos.CosDataEvent;
import org.sokybot.gameevents.events.cos.CosUpdateEvent;
import org.sokybot.gameevents.events.cos.CosUpdateType;
import org.sokybot.pet.api.IPetModel;
import org.sokybot.pet.api.IPetSnapshot;
import org.sokybot.pet.api.PetInfo;
import org.sokybot.pet.api.PetRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;

/**
 * Tracks COS pets for each machine from the reactive event bus.
 */
@Component(service = IPetModel.class, immediate = true)
public final class PetModelComponent implements IPetModel {

    private static final Logger log = LoggerFactory.getLogger(PetModelComponent.class);

    private final Map<String, MachinePetState> stateByMachine = new ConcurrentHashMap<>();
    private final List<Disposable> subscriptions = new ArrayList<>();

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Activate
    void activate() {
        subscriptions.add(reactiveEventBus.on(CharacterLoadedEvent.class).subscribe(this::onCharacterLoaded));
        subscriptions.add(reactiveEventBus.on(CosDataEvent.class).subscribe(this::onCosData));
        subscriptions.add(reactiveEventBus.on(CosUpdateEvent.class).subscribe(this::onCosUpdate));
        subscriptions.add(reactiveEventBus.on(CharacterDeathEvent.class).subscribe(this::onCharacterDeath));
    }

    @Deactivate
    void deactivate() {
        for (Disposable d : subscriptions) {
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
        }
        subscriptions.clear();
        stateByMachine.clear();
    }

    private static String normalize(String machineFullName) {
        if (machineFullName == null) {
            return null;
        }
        String t = machineFullName.trim();
        return t.isEmpty() ? null : t;
    }

    private void onCharacterLoaded(CharacterLoadedEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        stateByMachine.compute(key, (k, existing) -> {
            MachinePetState st = existing != null ? existing : new MachinePetState();
            int prev = st.ownerUniqueId;
            st.ownerUniqueId = e.getUniqueId();
            if (prev != 0 && prev != st.ownerUniqueId) {
                st.petsByEntityId.clear();
            }
            return st;
        });
    }

    private void onCosData(CosDataEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachinePetState st = stateByMachine.get(key);
        if (st == null || st.ownerUniqueId == 0) {
            return;
        }
        if (e.getOwnerUniqueId() != st.ownerUniqueId) {
            return;
        }
        PetRole role = PetRole.fromCosType(e.getCosType().getTypeId());
        if (role == PetRole.UNKNOWN) {
            return;
        }
        PetInfo info = PetInfo.builder()
                .entityUniqueId(e.getUniqueId())
                .objectId(e.getObjectId())
                .role(role)
                .hp(e.getHp())
                .maxHp(e.getMaxHp())
                .hunger(-1)
                .alive(true)
                .lastUpdateEpochMs(e.getTimestamp())
                .build();
        st.petsByEntityId.put(e.getUniqueId(), info);
    }

    private void onCosUpdate(CosUpdateEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachinePetState st = stateByMachine.get(key);
        if (st == null) {
            return;
        }
        CosUpdateType ut = e.getUpdateType();
        if (ut == CosUpdateType.TERMINATE) {
            st.petsByEntityId.remove(e.getUniqueId());
            return;
        }
        if (ut == CosUpdateType.HUNGER) {
            PetInfo prev = st.petsByEntityId.get(e.getUniqueId());
            if (prev == null) {
                return;
            }
            PetInfo updated = PetInfo.builder()
                    .entityUniqueId(prev.getEntityUniqueId())
                    .objectId(prev.getObjectId())
                    .role(prev.getRole())
                    .hp(prev.getHp())
                    .maxHp(prev.getMaxHp())
                    .hunger(e.getHungerPoints())
                    .alive(prev.isAlive())
                    .lastUpdateEpochMs(e.getTimestamp())
                    .build();
            st.petsByEntityId.put(e.getUniqueId(), updated);
        }
    }

    /**
     * Defensive: pets remain tracked after owner death until COS terminate.
     */
    private void onCharacterDeath(CharacterDeathEvent e) {
        if (log.isTraceEnabled()) {
            log.trace("CharacterDeathEvent for {} (pet projection state unchanged)", e.getFullName());
        }
    }

    @Override
    public Optional<IPetSnapshot> snapshot(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return Optional.empty();
        }
        MachinePetState st = stateByMachine.get(key);
        if (st == null) {
            return Optional.empty();
        }
        return Optional.of(PetSnapshots.fromState(st, key));
    }

    @Override
    public Optional<PetInfo> findActiveByRole(String machineFullName, PetRole role) {
        return snapshot(machineFullName).flatMap(s -> s.getActivePetByRole(role));
    }

    @Override
    public boolean isPetActive(String machineFullName, PetRole role) {
        return snapshot(machineFullName).map(s -> s.hasActivePet(role)).orElse(false);
    }
}
