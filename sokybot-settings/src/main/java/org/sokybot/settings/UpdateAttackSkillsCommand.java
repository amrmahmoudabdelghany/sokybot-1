package org.sokybot.settings;

import java.io.Serializable;
import java.util.List;
import org.sokybot.persistence.entities.MonsterType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAttackSkillsCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    private String machineId;
    private MonsterType monsterType;
    private List<String> skills;

    public MonsterType getMonsterType() {
        return monsterType;
    }

    public List<String> getSkills() {
        return skills;
    }

    public String getMachineId() {
        return machineId;
    }
}
