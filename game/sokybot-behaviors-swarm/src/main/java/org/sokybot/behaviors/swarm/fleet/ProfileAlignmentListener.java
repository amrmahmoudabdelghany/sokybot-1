package org.sokybot.behaviors.swarm.fleet;

import java.util.Map;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.settings.fleet.FleetMachineSettings;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmFleetAlignedEvent;
import org.sokybot.swarm.api.SwarmProfileUpdatedEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies {@link SwarmProfileUpdatedEvent} payloads to machines bound to the same profile (Epic #16).
 */
@Component(service = ProfileAlignmentListener.class, immediate = true)
public final class ProfileAlignmentListener {

    private static final Logger log = LoggerFactory.getLogger(ProfileAlignmentListener.class);

    @Reference
    private ISwarmEventBus swarmBus;

    @Reference
    private ISettingsRegistry settingsRegistry;

    @Reference
    private ISokybotContext sokybotContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Disposable subscription;

    @Activate
    void activate() {
        subscription = swarmBus.observe(SwarmProfileUpdatedEvent.class).subscribe(this::onProfileUpdated);
    }

    @Deactivate
    void deactivate() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    private void onProfileUpdated(SwarmProfileUpdatedEvent event) {
        if (event == null) {
            return;
        }
        String targetProfileId = event.getProfileId();
        String targetGroup = event.getGroupName();
        ISokybotContext ctx = sokybotContext;
        ISettingsRegistry reg = settingsRegistry;
        ISwarmEventBus bus = swarmBus;
        if (ctx == null || reg == null || bus == null) {
            return;
        }

        int successCount = 0;
        for (IGroupContext group : ctx.getGroups()) {
            if (group == null || !targetGroup.equals(group.name())) {
                continue;
            }
            for (IMachineContext machine : group.getMachines()) {
                if (machine == null) {
                    continue;
                }
                try {
                    if (alignMachineIfBound(group.name(), machine.getMachineName(), targetProfileId, event, reg)) {
                        successCount++;
                    }
                } catch (Exception ex) {
                    log.warn(
                            "Profile alignment failed for {}/{}: {}",
                            group.name(),
                            machine.getMachineName(),
                            ex.getMessage());
                }
            }
        }

        long now = System.currentTimeMillis();
        String reqId = UUID.randomUUID().toString();
        SwarmFleetAlignedEvent aligned = new SwarmFleetAlignedEvent(
                event.getRequesterMachineId(),
                now,
                reqId,
                targetProfileId,
                event.getExpectedMachineCount(),
                successCount);
        bus.publish(aligned);
    }

    /**
     * @return {@code true} when the machine is bound to the profile and every scope write succeeded
     */
    private boolean alignMachineIfBound(
            String groupName,
            String machineName,
            String targetProfileId,
            SwarmProfileUpdatedEvent event,
            ISettingsRegistry reg) {
        ISettingsProvider<FleetMachineSettings> fleetProvider = reg.getProvider(
                groupName,
                machineName,
                FleetMachineSettings.SCOPE_NAME,
                FleetMachineSettings.class);
        if (fleetProvider == null) {
            return false;
        }
        FleetMachineSettings fleet = fleetProvider.get();
        if (fleet == null || !targetProfileId.equals(fleet.getProfileId())) {
            return false;
        }

        boolean anyScope = false;
        for (Map.Entry<String, String> e : event.getScopeToSettingsJson().entrySet()) {
            String scope = e.getKey();
            String json = e.getValue();
            if (scope == null || scope.trim().isEmpty() || json == null) {
                continue;
            }
            anyScope = true;
            try {
                Map<String, Object> data = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                if (data == null || data.isEmpty()) {
                    continue;
                }
                reg.writeRawSettings(groupName, machineName, scope.trim(), data);
            } catch (JsonProcessingException jpe) {
                log.warn(
                        "Profile alignment: bad JSON for scope {} machine {}/{}: {}",
                        scope,
                        groupName,
                        machineName,
                        jpe.getMessage());
                return false;
            } catch (Exception ex) {
                log.warn(
                        "Profile alignment: writeRawSettings failed scope {} machine {}/{}: {}",
                        scope,
                        groupName,
                        machineName,
                        ex.getMessage());
                return false;
            }
        }
        return anyScope;
    }
}
