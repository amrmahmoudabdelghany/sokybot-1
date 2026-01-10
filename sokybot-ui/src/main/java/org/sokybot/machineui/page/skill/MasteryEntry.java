package org.sokybot.machineui.page.skill;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MasteryEntry {
    private int id;
    private String name;

    @Override
    public String toString() {
        return name;
    }
}
