package org.sokybot.party.api.caravan;

import lombok.Data;

/**
 * Epic #18 Caravan Syndicate: formation geometry and pacing (scope {@code caravan}).
 */
@Data
public class CaravanFormationSettings {

    private boolean caravanEnabled = false;

    private CaravanRole role = CaravanRole.NONE;

    private String caravanId = "";

    private float formationRadiusWorld = 15.0f;

    private float formationAngleDegrees = 0.0f;

    private float repositionEpsilonWorld = 2.0f;

    private long minMoveIntervalMs = 1000L;
}
