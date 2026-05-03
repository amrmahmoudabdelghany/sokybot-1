package org.sokybot.behaviors.logistics.internal.settings.quartermaster;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Epic #15 Grand Quartermaster: storage alignment policy (scope {@code quartermaster}).
 */
@Data
public class QuartermasterSettings {

    private boolean quartermasterEnabled = false;

    private String swarmStorageId = "guild_vault";

    private List<StorageRule> rules = new ArrayList<>();

    private int maxTabs = 5;

    private long lockTtlMs = 30000L;

    private long sortBudgetMovesPerTick = 3L;
}
