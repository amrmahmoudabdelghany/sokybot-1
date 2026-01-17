package org.sokybot.game.enums;

import java.util.Arrays;
import java.util.Optional;

import lombok.Getter;

@Getter
public enum SkillCastErrorType {
    
    SKILL_ON_COOLDOWN(0x05),
    INVALID_TARGET(0x06),
    OBSTACLE(0x10),
    WRONG_WEAPON(0x30),
    INSUFFICIENT_BOLTS(0x0E),
    UNKNOWN(-1);

    private final int value;

    SkillCastErrorType(int value) {
        this.value = value;
    }

    public static SkillCastErrorType of(int value) {
        return Arrays.stream(values())
                .filter(e -> e.value == value)
                .findFirst()
                .orElse(UNKNOWN);
    }
}
