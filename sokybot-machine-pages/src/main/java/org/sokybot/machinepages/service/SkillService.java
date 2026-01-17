package org.sokybot.machinepages.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.skill.SkillLevelUpEvent;
import org.sokybot.gameevents.events.skill.SkillPointsUpdateEvent;
import org.sokybot.gameevents.events.character.CharacterSkillLoadedEvent;
import org.sokybot.game.dto.Skill;
import org.sokybot.gamemodel.model.ITrainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Service for managing skills page state and handling skill-related game events.
 */
public class SkillService implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SkillService.class);
    
    private final String machineFullName;
    private final ITrainer trainer;
    private final Sinks.Many<Map<String, Object>> stateSink = 
        Sinks.many().multicast().onBackpressureBuffer(100);
    private volatile List<Map<String, Object>> currentSkills = new ArrayList<>();
    private volatile int skillPoints = 0;
    
    public SkillService(String machineFullName, ITrainer trainer) {
        this.machineFullName = machineFullName;
        this.trainer = trainer;
        // Load initial state
        refreshSkills();
    }
    
    @Override
    public void handleEvent(Event osgiEvent) {
        String fullName = (String) osgiEvent.getProperty("fullName");
        if (fullName == null || !machineFullName.equals(fullName)) {
            return; // Not for this machine
        }
        
        IGameEvent event = (IGameEvent) osgiEvent.getProperty("event");
        if (event instanceof SkillLevelUpEvent) {
            handleSkillLevelUp((SkillLevelUpEvent) event);
        } else if (event instanceof SkillPointsUpdateEvent) {
            handleSkillPointsUpdate((SkillPointsUpdateEvent) event);
        } else if (event instanceof CharacterSkillLoadedEvent) {
            handleSkillLoaded((CharacterSkillLoadedEvent) event);
        }
    }
    
    private void handleSkillLevelUp(SkillLevelUpEvent event) {
        if (event.isSuccess()) {
            logger.debug("Skill level up: id={}, name={}", event.getSkillId(), event.getSkillName());
            refreshSkills();
            emitStateUpdate();
        }
    }
    
    private void handleSkillPointsUpdate(SkillPointsUpdateEvent event) {
        logger.debug("Skill points updated: {}", event.getNewSkillPoints());
        this.skillPoints = event.getNewSkillPoints();
        refreshSkills();
        emitStateUpdate();
    }
    
    private void handleSkillLoaded(CharacterSkillLoadedEvent event) {
        logger.debug("Skill loaded: id={}, name={}, level={}", 
            event.getSkillId(), event.getSkillName(), event.getSkillLevel());
        refreshSkills();
        emitStateUpdate();
    }
    
    public Map<String, Object> handleAction(String action, Map<String, Object> data) {
        Map<String, Object> newState = new HashMap<>();
        switch (action) {
            case "refresh":
                refreshSkills();
                emitStateUpdate();
                newState.put("skills", new ArrayList<>(currentSkills));
                newState.put("skillPoints", skillPoints);
                return Map.of("success", true, "state", newState);
            case "enableSkill":
                // TODO: Implement skill enable/disable
                return Map.of("success", false, "error", "Not implemented");
            case "disableSkill":
                // TODO: Implement skill enable/disable
                return Map.of("success", false, "error", "Not implemented");
            default:
                return Map.of("success", false, "error", "Unknown action: " + action);
        }
    }
    
    public Flux<Map<String, Object>> streamSkills() {
        // Return initial state + updates
        return Flux.concat(
            Flux.just(getSkillsData()),
            stateSink.asFlux()
        );
    }
    
    private void refreshSkills() {
        List<Map<String, Object>> skills = new ArrayList<>();
        if (trainer != null && trainer.getSkills() != null) {
            for (Skill skill : trainer.getSkills()) {
                if (skill != null) {
                    Map<String, Object> skillData = new HashMap<>();
                    skillData.put("refId", String.valueOf(skill.getRefId()));
                    skillData.put("name", skill.getName());
                    skillData.put("level", skill.getSkillLvl());
                    skillData.put("enabled", skill.isEnabled());
                    skills.add(skillData);
                }
            }
            // Sort by refId
            skills.sort((a, b) -> Long.compare(
                Long.parseLong((String) a.get("refId")), 
                Long.parseLong((String) b.get("refId"))
            ));
        }
        currentSkills = skills;
    }
    
    private Map<String, Object> getSkillsData() {
        Map<String, Object> data = new HashMap<>();
        data.put("skills", new ArrayList<>(currentSkills));
        data.put("skillPoints", skillPoints);
        return data;
    }
    
    private void emitStateUpdate() {
        stateSink.tryEmitNext(getSkillsData());
    }
    
    public Map<String, Object> getInitialState() {
        return getSkillsData();
    }
    
    public void shutdown() {
        stateSink.tryEmitComplete();
    }
}
