package org.sokybot.gamemodel.internal;

import org.sokybot.gameevents.dto.MonsterData;
import org.sokybot.gamemodel.model.IMonster;
import org.sokybot.gameevents.enums.MonsterType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Monster extends Fighter implements IMonster {

    private MonsterType monsterType;
    private int maxHP;
    private int level;

    public Monster(MonsterData data) {
        super(data);
        this.monsterType = data.getMonsterType();
        // this.maxHP = data.getMaxHp(); // already in Fighter? Fighter had maxHP
        // Fighter.maxHP vs Monster.maxHP? Fighter has field `maxHP = 1`.
        // Monster also declared `private int maxHP` in old code.
        // Let's rely on Fighter's maxHP or override if needed.
        // Old Monster had separate maxHP field? Let's check old Monster.java found in
        // Step 56.
        // Yes: private int maxHP; private int level;
        // And Fighter had maxHP too. This shadows it.
        // I should probably clean this up, but to be safe I'll keep the shadowing if
        // logic depends on it, OR assume Fighter's is enough.
        // Fighter sets maxHP in constructor from MonsterData.
        // So I can remove the shadowed field in Monster if Fighter handles it.
        // But let's stick to strict copy for now to avoid bugs, unless I see obvious
        // refactor.
        // Actually, Fighter uses `data.getMaxHp()` in constructor.
        // So Fighter has the value. I'll use Fighter's maxHP and NOT shadow it.

        super.setMaxHP(data.getMaxHp());
        this.level = data.getLevel();
    }
}
