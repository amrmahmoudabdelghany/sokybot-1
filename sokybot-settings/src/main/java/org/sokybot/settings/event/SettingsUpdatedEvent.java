package org.sokybot.settings.event;

import org.sokybot.settings.Settings;
import lombok.Value;

@Value
public class SettingsUpdatedEvent {
    String groupName;
    String machineName;
    Settings settings;
}
