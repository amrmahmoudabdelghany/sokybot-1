package org.sokybot.machine.event;

import org.sokybot.game.dto.Mastery;
import org.springframework.context.ApplicationEvent;

public class MasteryLvlUpEvent extends ApplicationEvent {

    private final Mastery mastery;

    public MasteryLvlUpEvent(Object source, Mastery mastery) {
        super(source);
        this.mastery = mastery;
    }

    public Mastery getMastery() {
        return mastery;
    }
}
