package org.sokybot.gameevents.dto;

import lombok.Getter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PlayerData extends SpawnData {
    
   // Add fields as needed, e.g. guild name, items
   private final String guildName;
}
