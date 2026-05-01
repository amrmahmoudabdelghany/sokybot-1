package org.sokybot.party.projections.internal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.gameevents.events.character.CharacterLoadedEvent;
import org.sokybot.party.api.IPartyDirectory;
import org.sokybot.party.api.IPartyModel;
import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.settings.api.ISettingsRegistry;

import reactor.core.Disposable;

@Component(service = IPartyDirectory.class, immediate = true)
public final class PartyDirectoryComponent implements IPartyDirectory {

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Reference
    private IPartyModel partyModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ICombatModel combatModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISettingsRegistry settingsRegistry;

    private final ConcurrentMap<String, String> machineByCharacterName = new ConcurrentHashMap<>();
    private Disposable characterLoadedSub;

    @Activate
    void activate() {
        characterLoadedSub = reactiveEventBus.on(CharacterLoadedEvent.class).subscribe(this::onCharacterLoaded);
    }

    @Deactivate
    void deactivate() {
        if (characterLoadedSub != null && !characterLoadedSub.isDisposed()) {
            characterLoadedSub.dispose();
        }
        machineByCharacterName.clear();
    }

    @Override
    public Optional<String> resolveLeaderMachine(String followerMachineFullName) {
        String follower = normalize(followerMachineFullName);
        if (follower == null) {
            return Optional.empty();
        }

        String override = manualOverrideLeader(follower);
        if (override != null) {
            return Optional.of(override);
        }

        IPartySnapshot party = partyModel.snapshot(follower).orElse(null);
        if (party == null || party.getLeaderEntityId() <= 0) {
            return Optional.empty();
        }
        int leaderEntityId = party.getLeaderEntityId();

        ICombatModel combat = combatModel;
        if (combat == null) {
            return Optional.empty();
        }

        String groupPrefix = groupOf(follower);
        if (groupPrefix == null) {
            return Optional.empty();
        }

        for (String candidate : candidateMachineIds(party)) {
            if (candidate == null || !candidate.startsWith(groupPrefix + ".")) {
                continue;
            }
            ICombatSnapshot snap = combat.snapshot(candidate).orElse(null);
            if (snap == null) {
                continue;
            }
            Integer selfEntityId = snap.getSelfEntityId().orElse(null);
            if (selfEntityId != null && selfEntityId.intValue() == leaderEntityId) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    private void onCharacterLoaded(CharacterLoadedEvent event) {
        String machine = normalize(event.getFullName());
        String character = normalize(event.getCharacterName());
        if (machine != null && character != null) {
            machineByCharacterName.put(character, machine);
        }
    }

    private String manualOverrideLeader(String followerMachineFullName) {
        ISettingsRegistry registry = settingsRegistry;
        if (registry == null) {
            return null;
        }
        String[] parts = splitMachineId(followerMachineFullName);
        if (parts == null) {
            return null;
        }
        Map<String, Object> raw = registry.readRawSettings(parts[0], parts[1], "party");
        if (raw == null) {
            return null;
        }
        Object value = raw.get("leaderMachineFullName");
        if (!(value instanceof String)) {
            return null;
        }
        return normalize((String) value);
    }

    private java.util.List<String> candidateMachineIds(IPartySnapshot party) {
        java.util.List<String> out = new java.util.ArrayList<>();
        out.add(party.getMachineFullName());
        for (org.sokybot.party.api.PartyMember m : party.getMembers()) {
            String name = m.getCharName();
            if (name != null && !name.trim().isEmpty()) {
                String machineId = machineByCharacterName.get(name.trim());
                if (machineId != null) {
                    out.add(machineId);
                }
            }
        }
        return out;
    }

    private static String groupOf(String machineFullName) {
        int dot = machineFullName.indexOf('.');
        if (dot <= 0 || dot >= machineFullName.length() - 1) {
            return null;
        }
        return machineFullName.substring(0, dot);
    }

    private static String[] splitMachineId(String machineFullName) {
        int dot = machineFullName.indexOf('.');
        if (dot <= 0 || dot >= machineFullName.length() - 1) {
            return null;
        }
        return new String[] { machineFullName.substring(0, dot), machineFullName.substring(dot + 1) };
    }

    private static String normalize(String in) {
        if (in == null) {
            return null;
        }
        String t = in.trim();
        return t.isEmpty() ? null : t;
    }
}
