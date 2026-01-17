package org.sokybot.gameevents.events.skill;

import org.sokybot.game.enums.SkillCastErrorType;
import org.sokybot.gameevents.events.core.AbstractGameEvent;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class SkillCastErrorEvent extends AbstractGameEvent {

    private final SkillCastErrorType errorType;
    private final int rawError;

    public SkillCastErrorEvent(String fullName, SkillCastErrorType errorType, int rawError) {
        super(fullName);
        this.errorType = errorType;
        this.rawError = rawError;
    }
}
