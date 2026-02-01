package org.sokybot.gameevents.dto;

import org.sokybot.gameevents.enums.Rarity;
import lombok.Getter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ItemData extends SpawnData {
    
    private final int amount;
    private final boolean ownerExist;
    private final int ownerJID;
    private final byte plus;
    private final Rarity rarity;
}
