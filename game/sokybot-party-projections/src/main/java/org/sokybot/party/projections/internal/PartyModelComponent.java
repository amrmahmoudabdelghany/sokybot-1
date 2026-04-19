package org.sokybot.party.projections.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.events.character.CharacterLoadedEvent;
import org.sokybot.gameevents.events.entity.EntityStateUpdateEvent;
import org.sokybot.gameevents.events.party.PartyInviteEvent;
import org.sokybot.gameevents.events.party.PartyMatchingEvent;
import org.sokybot.gameevents.events.party.PartyMatchingListEvent;
import org.sokybot.gameevents.events.party.PartyUpdateEvent;
import org.sokybot.party.api.IPartyModel;
import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.party.api.PartyClass;
import org.sokybot.party.api.PartyMatchListing;
import org.sokybot.party.api.PartyMember;
import org.sokybot.party.api.PartyRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Party roster overlay sourced from {@link IReactiveEventBus}; keyed by machine full name.
 */
@Component(service = IPartyModel.class, immediate = true)
public final class PartyModelComponent implements IPartyModel {

    private static final Logger log = LoggerFactory.getLogger(PartyModelComponent.class);

    private final ConcurrentMap<String, MachinePartyState> stateByMachine = new ConcurrentHashMap<>();

    private final List<Disposable> subscriptions = new ArrayList<>();

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Activate
    void activate() {
        subscriptions.add(reactiveEventBus.on(CharacterLoadedEvent.class).subscribe(this::onCharacterLoaded));
        subscriptions.add(reactiveEventBus.on(PartyInviteEvent.class).subscribe(this::onPartyInvite));
        subscriptions.add(reactiveEventBus.on(PartyUpdateEvent.class).subscribe(this::onPartyUpdate));
        subscriptions.add(reactiveEventBus.on(PartyMatchingEvent.class).subscribe(this::onPartyMatchingEvent));
        subscriptions.add(reactiveEventBus.on(PartyMatchingListEvent.class).subscribe(this::onPartyMatchingListEvent));
        subscriptions.add(reactiveEventBus.on(EntityStateUpdateEvent.class).subscribe(this::onEntityState));

        log.debug("IPartyModel projection active");
    }

    @Deactivate
    void deactivate() {
        for (Disposable d : subscriptions) {
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
        }
        subscriptions.clear();
        for (MachinePartyState st : stateByMachine.values()) {
            st.snapshotSink.tryEmitComplete();
        }
        stateByMachine.clear();
    }

    private static String normalize(String machineFullName) {
        if (machineFullName == null) {
            return null;
        }
        String t = machineFullName.trim();
        return t.isEmpty() ? null : t;
    }

    private MachinePartyState stateFor(String fullNameKey) {
        return stateByMachine.computeIfAbsent(fullNameKey, fn -> new MachinePartyState());
    }

    private void publishSnapshot(String fullNameKey, MachinePartyState st) {
        if (fullNameKey == null || st == null) {
            return;
        }
        st.snapshotSink.emitNext(buildSnapshot(fullNameKey, st), Sinks.EmitFailureHandler.FAIL_FAST);
    }

    private void onCharacterLoaded(CharacterLoadedEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachinePartyState st = stateFor(key);
        st.selfEntityId = Integer.valueOf(e.getUniqueId());
        publishSnapshot(key, st);
    }

    private void onPartyInvite(PartyInviteEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachinePartyState st = stateFor(key);
        st.lastInviteEpochMs = e.getTimestamp();
        publishSnapshot(key, st);
    }

    private void onPartyUpdate(PartyUpdateEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachinePartyState st = stateFor(key);
        byte t = e.getUpdateType();
        if (t == PartyUpdateEvent.TYPE_DISMISSED) {
            resetParty(st);
            publishSnapshot(key, st);
            return;
        }
        if (t == PartyUpdateEvent.TYPE_LEAVE) {
            Integer uid = e.getMemberUniqueId();
            if (uid != null) {
                st.members.remove(uid.intValue());
                if (st.leaderEntityId == uid.intValue()) {
                    st.leaderEntityId = 0;
                }
            }
            publishSnapshot(key, st);
            return;
        }
        if (t == PartyUpdateEvent.TYPE_JOINED || t == PartyUpdateEvent.TYPE_MEMBER
                || t == PartyUpdateEvent.TYPE_LEADER || t == PartyUpdateEvent.TYPE_LEADER_CHANGE) {
            Integer uid = e.getMemberUniqueId();
            if (uid != null) {
                TacticalPartyMember tm = st.members.computeIfAbsent(uid.intValue(), TacticalPartyMember::new);
                applyMemberOverlay(e, tm);
                if (t == PartyUpdateEvent.TYPE_LEADER || t == PartyUpdateEvent.TYPE_LEADER_CHANGE) {
                    st.leaderEntityId = uid.intValue();
                }
            }
        }
        if (st.formedAtEpochMs <= 0L && !st.members.isEmpty()) {
            st.formedAtEpochMs = e.getTimestamp();
        }
        publishSnapshot(key, st);
    }

    private static void applyMemberOverlay(PartyUpdateEvent e, TacticalPartyMember tm) {
        if (e.getMemberName() != null) {
            tm.charName = e.getMemberName();
        }
        if (e.getMemberLevel() != null) {
            tm.level = e.getMemberLevel().intValue() & 0xFF;
        }
        Byte hm = e.getMemberHealthMana();
        if (hm != null) {
            int[] hpMp = hpMpPercents(hm.byteValue());
            tm.hpPercent = hpMp[0];
            tm.mpPercent = hpMp[1];
        }
        tm.lastUpdateEpochMs = e.getTimestamp();
    }

    /** Best-effort decode of packed HP|MP nibbles from party roster updates. */
    private static int[] hpMpPercents(byte packed) {
        int v = packed & 0xFF;
        int hi = (v >> 4) & 0x0F;
        int lo = v & 0x0F;
        return new int[] { Math.min(100, hi * 10), Math.min(100, lo * 10) };
    }

    private static void resetParty(MachinePartyState st) {
        st.members.clear();
        st.partyId = 0;
        st.leaderEntityId = 0;
        st.formedAtEpochMs = 0L;
        st.partyPvpEnabled = false;
        st.expShareEnabled = false;
    }

    private void onPartyMatchingEvent(PartyMatchingEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachinePartyState st = stateFor(key);
        PartyMatchingEvent.MatchingEventType et = e.getEventType();
        if (et == PartyMatchingEvent.MatchingEventType.PARTY_CREATED && e.getPartyId() > 0) {
            st.partyId = e.getPartyId();
            if (st.formedAtEpochMs <= 0L) {
                st.formedAtEpochMs = e.getTimestamp();
            }
        }
        publishSnapshot(key, st);
    }

    private void onPartyMatchingListEvent(PartyMatchingListEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachinePartyState st = stateFor(key);
        st.replaceMatchingEntries(e.getEntries());
        publishSnapshot(key, st);
    }

    private void onEntityState(EntityStateUpdateEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachinePartyState st = stateByMachine.get(key);
        if (st == null) {
            return;
        }
        TacticalPartyMember tm = st.members.get(e.getEntityId());
        if (tm == null) {
            return;
        }
        Integer hp = e.getCurrentHP();
        Integer mp = e.getCurrentMP();
        if (hp != null) {
            tm.currentHp = hp.intValue();
            tm.hpPercent = pct(tm.currentHp, tm.maxHp);
        }
        if (mp != null) {
            tm.currentMp = mp.intValue();
            tm.mpPercent = pct(tm.currentMp, tm.maxMp);
        }
        tm.lastUpdateEpochMs = e.getTimestamp();
        publishSnapshot(key, st);
    }

    private static int pct(int cur, int max) {
        if (max <= 0 || cur < 0) {
            return -1;
        }
        return (int) Math.min(100L, (100L * cur) / max);
    }

    @Override
    public Optional<IPartySnapshot> snapshot(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return Optional.empty();
        }
        MachinePartyState st = stateByMachine.get(key);
        if (st == null) {
            return Optional.empty();
        }
        return Optional.of(buildSnapshot(key, st));
    }

    @Override
    public Flux<IPartySnapshot> observe(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return Flux.empty();
        }
        MachinePartyState st = stateFor(key);
        Flux<IPartySnapshot> tail = st.snapshotSink.asFlux().sample(Duration.ofMillis(50));
        Optional<IPartySnapshot> seed = snapshot(machineFullName);
        if (seed.isPresent()) {
            return Flux.concat(Flux.just(seed.get()), tail);
        }
        return tail;
    }

    private IPartySnapshot buildSnapshot(String machineFullName, MachinePartyState st) {
        List<PartyMember> list = new ArrayList<>();
        final int leaderId = st.leaderEntityId;
        for (TacticalPartyMember tm : st.members.values()) {
            PartyRole role = leaderId > 0 && tm.entityId == leaderId ? PartyRole.LEADER : PartyRole.MEMBER;
            int hpPct = tm.hpPercent >= 0 ? tm.hpPercent : pct(tm.currentHp, tm.maxHp);
            int mpPct = tm.mpPercent >= 0 ? tm.mpPercent : pct(tm.currentMp, tm.maxMp);
            list.add(new PartyMember(tm.entityId, tm.charName, tm.level, hpPct, mpPct, role, tm.partyClass,
                    tm.x, tm.y, tm.z, tm.lastUpdateEpochMs));
        }
        list.sort(Comparator.comparingInt((PartyMember m) -> leaderId > 0 && m.getEntityId() == leaderId ? 0 : 1)
                .thenComparingInt(PartyMember::getEntityId));

        return PartySnapshot.builder(machineFullName).partyId(st.partyId).leaderEntityId(st.leaderEntityId)
                .members(list).formedAtEpochMs(st.formedAtEpochMs).partyPvpEnabled(st.partyPvpEnabled)
                .expShareEnabled(st.expShareEnabled).lastInviteEpochMs(st.lastInviteEpochMs)
                .matchingListings(mapMatchingList(st.lastMatchingEntries)).build();
    }

    private static List<PartyMatchListing> mapMatchingList(
            List<PartyMatchingListEvent.PartyMatchEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        List<PartyMatchListing> out = new ArrayList<>(entries.size());
        for (PartyMatchingListEvent.PartyMatchEntry e : entries) {
            if (e == null) {
                continue;
            }
            out.add(new PartyMatchListing(e.getPartyNumber(), e.getMasterName(), e.getTitle(),
                    e.getLevelMin() & 0xFF, e.getLevelMax() & 0xFF));
        }
        return Collections.unmodifiableList(out);
    }
}
