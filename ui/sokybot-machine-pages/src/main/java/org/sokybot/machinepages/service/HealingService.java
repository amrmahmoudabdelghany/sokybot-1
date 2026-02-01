package org.sokybot.machinepages.service;

import java.util.HashMap;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sokybot.actuator.training.TrainingSettings;
import org.sokybot.gamemodel.model.IFighter;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.runtime.IGameStateProvider;
import org.sokybot.settings.api.IProfileManager;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Service for managing healing page state (HP/MP settings + status).
 */
public class HealingService implements EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HealingService.class);

    private final String machineFullName;
    private final IGameStateProvider machineContext;
    private final ISettingsProvider<TrainingSettings> settingsProvider;
    private final Sinks.Many<Map<String, Object>> stateSink = Sinks.many().multicast().onBackpressureBuffer(100);

    public HealingService(
            String machineFullName,
            IGameStateProvider machineContext,
            ISettingsRegistry settingsRegistry) {

        this.machineFullName = machineFullName;
        this.machineContext = machineContext;

        // Healing settings are part of TrainingSettings
        this.settingsProvider = settingsRegistry.getProvider(
                machineContext.getGroupName(),
                machineContext.getMachineName(),
                "training",
                TrainingSettings.class);

        // Subscribe to settings changes
        this.settingsProvider.subscribe(settings -> emitStateUpdate());
    }

    @Override
    public void handleEvent(Event osgiEvent) {
        String fullName = (String) osgiEvent.getProperty("fullName");
        if (fullName == null || !machineFullName.equals(fullName)) {
            return;
        }

        // Handle HP/MP update events
        String topic = osgiEvent.getTopic();
        if (topic.contains("UpdateHP") || topic.contains("UpdateMP")) {
            emitStateUpdate();
        }
    }

    public Map<String, Object> handleAction(String action, Map<String, Object> data) {
        try {
            switch (action) {
                case "refresh":
                    break;
                case "save":
                    settingsProvider.save();
                    break;
                case "update":
                    settingsProvider.update(settings -> {
                        if (data.containsKey("hpPotionThreshold"))
                            settings.setHpPotionThreshold(((Number) data.get("hpPotionThreshold")).intValue());
                        if (data.containsKey("mpPotionThreshold"))
                            settings.setMpPotionThreshold(((Number) data.get("mpPotionThreshold")).intValue());
                        if (data.containsKey("useHpPotion"))
                            settings.setUseHpPotion((Boolean) data.get("useHpPotion"));
                        if (data.containsKey("useMpPotion"))
                            settings.setUseMpPotion((Boolean) data.get("useMpPotion"));
                    });
                    break;
                default:
                    return Map.of("success", false, "error", "Unknown action: " + action);
            }

            return Map.of("success", true, "state", getState());

        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    public Flux<Map<String, Object>> streamHealing() {
        return Flux.concat(
                Flux.just(getState()),
                stateSink.asFlux());
    }

    private Map<String, Object> getState() {
        Map<String, Object> data = new HashMap<>();

        // Settings
        TrainingSettings settings = settingsProvider.get();
        data.put("settings", settings);
        data.put("isDirty", settingsProvider.isDirty());

        // Game State (HP/MP)
        try {
            ITrainer trainer = machineContext.getGameModel().getTrainer();
            if (trainer instanceof IFighter) {
                IFighter fighter = (IFighter) trainer;
                data.put("currentHP", fighter.getCurrentHP());
                data.put("maxHP", fighter.getMaxHP());
                data.put("currentMP", fighter.getCurrentMP());
                data.put("maxMP", fighter.getMaxMP());
            }
        } catch (Exception e) {
            // Ignore if game model not ready
        }

        return data;
    }

    private void emitStateUpdate() {
        stateSink.tryEmitNext(getState());
    }

    public Map<String, Object> getInitialState() {
        return getState();
    }

    public void shutdown() {
        stateSink.tryEmitComplete();
    }
}
