package org.sokybot.machineui.page.skill;

import javax.swing.Icon;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SkillTableEntry {
    protected Icon skillIcon;
    protected String skillName;
    protected int skillLvl;
}
