package org.sokybot.behaviors.party.internal;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.party.internal.settings.PartySettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.navigation.api.INavigator;
import org.sokybot.navigation.api.NavigationException;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.party.api.PartyCycleKeys;
import org.sokybot.swarm.api.RosterBlackboardKeys;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;

/**
 * Recruit walks toward {@link RosterBlackboardKeys#KEY_ROSTER_RALLY_DESTINATION} during party cycle (Epic #17).
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE, property = "order=2")
public final class RosterRallyPartyBehavior implements IBehavior<PartySettings> {

    private static final float ARRIVAL_RADIUS = 5.0f;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile INavigator navigator;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Override
    public String id() {
        return PartyCycleKeys.BEHAVIOR_ROSTER_RALLY_PARTY;
    }

    @Override
    public int order() {
        return 2;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return PartyCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<PartySettings> settingsType() {
        return PartySettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, PartySettings settings) {
        if (context == null) {
            return false;
        }
        Object raw = context.getPersistentData().get(RosterBlackboardKeys.KEY_ROSTER_RALLY_DESTINATION);
        return raw instanceof WorldPoint;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, PartySettings settings) {
        Map<String, Object> pd = context.getPersistentData();
        Object raw = pd.get(RosterBlackboardKeys.KEY_ROSTER_RALLY_DESTINATION);
        if (!(raw instanceof WorldPoint)) {
            return BehaviorStatus.SKIPPED;
        }
        WorldPoint dest = (WorldPoint) raw;
        INavigator nav = navigator;
        if (nav == null) {
            return BehaviorStatus.SKIPPED;
        }
        ITownModel tm = townModel;
        if (tm != null) {
            try {
                ITownSnapshot snap = tm.snapshot(context.getMachineId()).orElse(null);
                if (snap != null) {
                    WorldPoint self = new WorldPoint(snap.getSelfX(), snap.getSelfY(), snap.getSelfZ());
                    if (self.distanceTo(dest) <= ARRIVAL_RADIUS) {
                        return BehaviorStatus.EXECUTED;
                    }
                }
            } catch (Exception ignored) {
                // fall through to walk attempt
            }
        }
        try {
            nav.walkTo(context, dest);
        } catch (NavigationException ex) {
            context.log("WARN", "Roster rally walk failed: {}", ex.getMessage());
        }
        return BehaviorStatus.EXECUTED;
    }
}
