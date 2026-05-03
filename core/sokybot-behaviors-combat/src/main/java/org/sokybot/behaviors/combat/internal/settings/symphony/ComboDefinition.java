package org.sokybot.behaviors.combat.internal.settings.symphony;

import lombok.Data;

/**
 * Epic #21: one step in a symphony combo chain (trigger skill → payload skill).
 */
@Data
public class ComboDefinition {

    private String comboId;

    private int triggerSkillRefId;

    private int payloadSkillRefId;

    private int expectedStatusRefId;

    private long maxWaitTimeMs = 3000L;

    private float targetMatchRadiusWorld = 12.0f;
}
