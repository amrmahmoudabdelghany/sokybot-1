package org.sokybot.settings;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.sokybot.settings.event.SettingsUpdatedEvent;
import org.sokybot.settings.UpdateAttackSkillsCommand;

import java.util.HashMap;
import java.util.Map;

@Component(
    service = EventHandler.class,
    property = {
        org.osgi.service.event.EventConstants.EVENT_TOPIC + "=sokybot/command/settings/update"
    }
)
public class SettingsCommandHandler implements EventHandler {

    private ISettingsManager settingsManager;
    private EventAdmin eventAdmin;

    @Reference
    public void setSettingsManager(ISettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    @Reference
    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    @Override
    public void handleEvent(Event event) {
        Object prop = event.getProperty("command");
        
        // We expect the Event to carry group and machine info, 
        // OR the command itself to have it.
        // Currently UpdateAttackSkillsCommand has machineId.
        // But ISettingsManager needs groupName and machineName.
        // Assuming machineId == trainerName, and we need groupName.
        // The UI should send these in the Event properties.
        
        String groupName = (String) event.getProperty("groupName");
        String machineName = (String) event.getProperty("machineName"); // or trainerName

        if (prop instanceof UpdateAttackSkillsCommand && groupName != null && machineName != null) {
            UpdateAttackSkillsCommand cmd = (UpdateAttackSkillsCommand) prop;
            
            // 1. Load Scope "core"
            Settings settings = settingsManager.loadSettings(groupName, machineName, "core", Settings.class);
            
            // 2. Modify
            settings.replaceAttackSkills(cmd.getMonsterType(), cmd.getSkills());
            
            // 3. Save
            settingsManager.saveSettings(groupName, machineName, "core", settings);
            
            // 4. Publish Update Event
            Map<String, Object> props = new HashMap<>();
            props.put("groupName", groupName);
            props.put("machineName", machineName);
            props.put("settings", settings);
            eventAdmin.postEvent(new Event("sokybot/settings/updated", props));
        }
    }
}
