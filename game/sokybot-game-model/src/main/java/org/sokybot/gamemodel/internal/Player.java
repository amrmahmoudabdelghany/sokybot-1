package org.sokybot.gamemodel.internal;

import org.sokybot.game.dto.PlayerData;
import org.sokybot.game.dto.SpawnData;
import org.sokybot.gamemodel.model.IPlayer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Player extends Fighter implements IPlayer {

    public Player(SpawnData data) {
        super(data);
        if(data instanceof PlayerData) {
            // Apply player data
        }
    }
}
