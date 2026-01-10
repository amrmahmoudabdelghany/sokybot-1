package org.sokybot.machineui.page.skill;

import org.sokybot.persistence.entities.MonsterType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonsterTypeEntry {
    private String name;
    private MonsterType type;

    @Override
    public String toString() {
        return name;
    }
}
