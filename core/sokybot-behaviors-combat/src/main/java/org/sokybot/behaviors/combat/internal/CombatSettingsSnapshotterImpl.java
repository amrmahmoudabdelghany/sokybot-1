package org.sokybot.behaviors.combat.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.ICombatSettings;
import org.sokybot.combat.api.ICombatSettingsSnapshotter;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;

/**
 * Resolves machine-scoped {@link CombatSettings} from the settings registry.
 */
@Component(service = ICombatSettingsSnapshotter.class, immediate = true)
public final class CombatSettingsSnapshotterImpl implements ICombatSettingsSnapshotter {

    @Override
    @SuppressWarnings("unused")
    public ICombatSettings snapshot(IWorkflowContext context, boolean forceRefresh) {
        ISettingsRegistry registry = context.getService(ISettingsRegistry.class);
        if (registry == null) {
            return new CombatSettings();
        }
        ISettingsProvider<CombatSettings> provider = registry.getProvider(context.getGroupName(),
                context.getMachineName(), "combat", CombatSettings.class);
        CombatSettings s = provider != null ? provider.get() : null;
        return s != null ? s : new CombatSettings();
    }
}
