package org.sokybot.behaviors.swarm.fleet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.settings.fleet.FleetMachineSettings;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmProfileUpdatedEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes {@link SwarmProfileUpdatedEvent} after reading a group profile JSON from disk (Epic #16).
 */
@Component(
        immediate = true,
        service = { GlobalProfileCoordinator.class, EventHandler.class },
        property = EventConstants.EVENT_TOPIC + "=org/sokybot/profile/SAVED")
public final class GlobalProfileCoordinator implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalProfileCoordinator.class);

    private static final String DATA_DIR = "sokybot-data";
    private static final String SETTINGS_DIR = "settings";
    private static final String PROFILES_DIR = "profiles";

    @Reference
    private ISwarmEventBus swarmBus;

    @Reference
    private ISokybotContext sokybotContext;

    @Reference
    private ISettingsRegistry settingsRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reads {@code sokybot-data/settings/{group}/profiles/{profileId}.json} (same layout as {@link org.sokybot.settings.internal.ProfileManagerImpl})
     * and broadcasts the parsed scope payloads.
     */
    public void publishProfileUpdated(String groupName, String profileId, String requesterMachineId) {
        if (groupName == null || groupName.trim().isEmpty() || profileId == null || profileId.trim().isEmpty()) {
            log.warn("publishProfileUpdated: invalid groupName or profileId");
            return;
        }
        String safeGroup = sanitize(groupName);
        String safeProfile = sanitize(profileId);
        Path profileFile = Paths.get(DATA_DIR, SETTINGS_DIR, safeGroup, PROFILES_DIR, safeProfile + ".json");
        if (!Files.isRegularFile(profileFile)) {
            log.warn("publishProfileUpdated: profile file not found: {}", profileFile);
            return;
        }

        long profileVersionEpochMs;
        try {
            profileVersionEpochMs = Files.getLastModifiedTime(profileFile).toMillis();
        } catch (IOException e) {
            log.warn("publishProfileUpdated: cannot read mtime for {}: {}", profileFile, e.getMessage());
            profileVersionEpochMs = System.currentTimeMillis();
        }

        Map<String, String> scopeToSettingsJson;
        try {
            scopeToSettingsJson = buildScopePayloads(profileFile);
        } catch (JsonProcessingException e) {
            log.warn("publishProfileUpdated: JSON error for {}: {}", profileFile, e.getMessage());
            return;
        } catch (IOException e) {
            log.warn("publishProfileUpdated: IO error reading {}: {}", profileFile, e.getMessage());
            return;
        }

        if (scopeToSettingsJson.isEmpty()) {
            log.warn("publishProfileUpdated: no scope entries parsed from {}", profileFile);
            return;
        }

        int expectedMachineCount = countMachinesBoundToProfile(groupName.trim(), profileId.trim());

        String rid = requesterMachineId == null || requesterMachineId.trim().isEmpty()
                ? "coordinator"
                : requesterMachineId.trim();
        String reqId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        SwarmProfileUpdatedEvent event = new SwarmProfileUpdatedEvent(
                rid,
                now,
                reqId,
                profileId.trim(),
                groupName.trim(),
                profileId.trim(),
                scopeToSettingsJson,
                profileVersionEpochMs,
                expectedMachineCount);
        swarmBus.publish(event);
        log.info(
                "Published SwarmProfileUpdatedEvent profileId={} group={} scopes={} expectedMachines={}",
                profileId.trim(),
                groupName.trim(),
                Integer.valueOf(scopeToSettingsJson.size()),
                Integer.valueOf(expectedMachineCount));
    }

    private Map<String, String> buildScopePayloads(Path profileFile) throws IOException, JsonProcessingException {
        JsonNode root = objectMapper.readTree(profileFile.toFile());
        if (root == null || !root.isObject()) {
            return Map.of();
        }
        Map<String, String> out = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            String scope = e.getKey();
            JsonNode node = e.getValue();
            if (scope == null || scope.trim().isEmpty() || node == null || node.isNull()) {
                continue;
            }
            out.put(scope.trim(), objectMapper.writeValueAsString(node));
        }
        return out;
    }

    private int countMachinesBoundToProfile(String groupName, String profileId) {
        ISokybotContext ctx = sokybotContext;
        ISettingsRegistry reg = settingsRegistry;
        if (ctx == null || reg == null) {
            return 0;
        }
        int count = 0;
        for (IGroupContext group : ctx.getGroups()) {
            if (group == null || !groupName.equals(group.name())) {
                continue;
            }
            for (IMachineContext machine : group.getMachines()) {
                if (machine == null) {
                    continue;
                }
                try {
                    ISettingsProvider<FleetMachineSettings> provider = reg.getProvider(
                            group.name(),
                            machine.getMachineName(),
                            FleetMachineSettings.SCOPE_NAME,
                            FleetMachineSettings.class);
                    if (provider == null) {
                        continue;
                    }
                    FleetMachineSettings fs = provider.get();
                    if (fs != null && profileId.equals(fs.getProfileId())) {
                        count++;
                    }
                } catch (Exception ex) {
                    log.debug(
                            "countMachinesBoundToProfile: skip machine {}/{}: {}",
                            group.name(),
                            machine.getMachineName(),
                            ex.getMessage());
                }
            }
        }
        return count;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    @Override
    public void handleEvent(Event event) {
        if (event == null) {
            return;
        }
        Object g = event.getProperty("groupName");
        Object p = event.getProperty("profileId");
        if (!(g instanceof String) || !(p instanceof String)) {
            log.warn("handleEvent: missing groupName or profileId properties on topic {}", event.getTopic());
            return;
        }
        publishProfileUpdated((String) g, (String) p, String.valueOf(System.currentTimeMillis()));
    }
}
