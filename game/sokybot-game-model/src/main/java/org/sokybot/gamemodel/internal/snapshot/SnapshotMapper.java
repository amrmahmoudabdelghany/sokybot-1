package org.sokybot.gamemodel.internal.snapshot;

import java.util.Optional;

import org.sokybot.gamemodel.model.IItem;
import org.sokybot.gamemodel.model.IMonster;
import org.sokybot.gamemodel.model.IPlayer;
import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.gamemodel.model.ITrainer;

public final class SnapshotMapper {
    private SnapshotMapper() {
    }

    public static Optional<ISpawn> toSnapshot(ISpawn spawn) {
        if (spawn == null) {
            return Optional.empty();
        }
        if (spawn instanceof ITrainer) {
            return Optional.of(TrainerSnapshot.of((ITrainer) spawn));
        }
        if (spawn instanceof IMonster) {
            return Optional.of(MonsterSnapshot.of((IMonster) spawn));
        }
        if (spawn instanceof IPlayer) {
            return Optional.of(PlayerSnapshot.of((IPlayer) spawn));
        }
        if (spawn instanceof IItem) {
            return Optional.of(ItemSnapshot.of((IItem) spawn));
        }
        return Optional.empty();
    }
}
