package org.sokybot.machine.event.trainerevent;

import org.sokybot.game.dto.Skill;
import org.springframework.context.ApplicationEvent;

public class SkillLvlupEvent extends ApplicationEvent {

    private final Skill skill;

    public SkillLvlupEvent(Skill skill) {
        super(skill);
        this.skill = skill;
    }

    public Skill getSkill() {
        return skill;
    }
}
