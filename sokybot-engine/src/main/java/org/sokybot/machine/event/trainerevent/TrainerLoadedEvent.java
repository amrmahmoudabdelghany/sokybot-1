package org.sokybot.machine.event.trainerevent;

import org.sokybot.machine.model.Trainer;
import org.springframework.context.ApplicationEvent;

public class TrainerLoadedEvent extends ApplicationEvent {

    private final Trainer trainer;

    public TrainerLoadedEvent(Trainer trainer) {
        super(trainer);
        this.trainer = trainer;
    }

    public Trainer getTrainer() {
        return trainer;
    }
}
