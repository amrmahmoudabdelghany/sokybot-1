package org.sokybot.behaviors.combat.internal.settings.symphony;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Epic #21 Executioner's Symphony: distributed combo configuration (scope {@code symphony}).
 */
@Data
public class SymphonyComboSettings {

    private boolean symphonyEnabled = false;

    private List<ComboDefinition> combos = new ArrayList<>();
}
