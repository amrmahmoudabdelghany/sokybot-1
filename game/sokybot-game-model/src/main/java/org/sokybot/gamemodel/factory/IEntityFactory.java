package org.sokybot.gamemodel.factory;

import org.sokybot.gamemodel.model.IItem;
import org.sokybot.gamemodel.model.IMonster;
import org.sokybot.gamemodel.model.IPlayer;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.gamemodel.spec.ItemSpec;
import org.sokybot.gamemodel.spec.MonsterSpec;
import org.sokybot.gamemodel.spec.PlayerSpec;
import org.sokybot.gamemodel.spec.TrainerSpec;

public interface IEntityFactory {

    ITrainer createTrainer(TrainerSpec spec);

    IMonster createMonster(MonsterSpec spec);

    IPlayer createPlayer(PlayerSpec spec);

    IItem createItem(ItemSpec spec);
}
