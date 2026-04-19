package org.sokybot.behaviors.party.internal;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.party.internal.settings.PartySettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.party.api.IPartyModel;
import org.sokybot.party.api.IPartyPolicy;
import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.party.api.PartyCycleKeys;
import org.sokybot.party.api.PartyMatchListing;
import org.sokybot.party.coordination.api.IPartyCoordinator;

/**
 * Invites matching-list candidates whose title matches configured matching title.
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class AutoInviteBehavior implements IBehavior<PartySettings> {

    private static final long PER_CANDIDATE_COOLDOWN_MS = 60_000L;

    private static final String PD_MATCH_FORM_SENT = "party.autoInvite.matchFormSent";

    @Reference
    private IPartyModel partyModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IPartyCoordinator partyCoordinator;

    @Override
    public String id() {
        return PartyCycleKeys.BEHAVIOR_AUTO_INVITE;
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
        if (settings == null) {
            return false;
        }
        IPartyPolicy policy = settings.toPolicy();
        if (!policy.isAutoInviteFromMatching()) {
            return false;
        }
        Optional<IPartySnapshot> snap = partyModel.snapshot(context.getMachineId());
        return snap.isPresent() && !snap.get().getMatchingListings().isEmpty();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, PartySettings settings) {
        if (settings == null) {
            return BehaviorStatus.SKIPPED;
        }
        IPartyPolicy policy = settings.toPolicy();
        Optional<IPartySnapshot> snapOpt = partyModel.snapshot(context.getMachineId());
        if (!snapOpt.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        IPartySnapshot snap = snapOpt.get();
        Map<String, Object> pd = context.getPersistentData();

        maybeSendMatchingForm(context, settings, policy, pd);

        String targetTitle = policy.getMatchingTitle();
        if (targetTitle == null || targetTitle.trim().isEmpty()) {
            return BehaviorStatus.SKIPPED;
        }
        String normTarget = targetTitle.trim().toLowerCase(Locale.ROOT);

        long now = System.currentTimeMillis();
        for (PartyMatchListing row : snap.getMatchingListings()) {
            if (row == null) {
                continue;
            }
            String title = row.getTitle();
            if (title == null || title.trim().isEmpty()) {
                continue;
            }
            if (!title.trim().toLowerCase(Locale.ROOT).contains(normTarget)) {
                continue;
            }
            String master = row.getMasterName();
            if (master == null || master.trim().isEmpty()) {
                continue;
            }
            String key = PartyCycleKeys.KEY_AUTO_INVITE_PREFIX + master.trim().toLowerCase(Locale.ROOT);
            Object raw = pd.get(key);
            long last = raw instanceof Number ? ((Number) raw).longValue() : 0L;
            if (now - last < PER_CANDIDATE_COOLDOWN_MS) {
                continue;
            }
            if (partyCoordinator != null) {
                partyCoordinator.requestInvite(context.getMachineId(), master.trim());
            }
            PartyPackets.sendInvite(context, master.trim());
            pd.put(key, Long.valueOf(now));
            return BehaviorStatus.EXECUTED;
        }
        return BehaviorStatus.SKIPPED;
    }

    private void maybeSendMatchingForm(IWorkflowContext ctx, PartySettings settings, IPartyPolicy policy,
            Map<String, Object> pd) {
        if (!policy.isAutoCreateMatch()) {
            return;
        }
        Object sent = pd.get(PD_MATCH_FORM_SENT);
        if (Boolean.TRUE.equals(sent)) {
            return;
        }
        String title = settings.getMatchingTitle();
        if (title == null || title.trim().isEmpty()) {
            return;
        }
        PartyPackets.sendMatchingForm(ctx, title.trim(), 1, 99);
        pd.put(PD_MATCH_FORM_SENT, Boolean.TRUE);
        if (partyCoordinator != null) {
            partyCoordinator.registerMatchingPost(ctx.getMachineId(), title.trim(), 1, 99);
        }
    }

    @Override
    public long postDelayMs() {
        return 400L;
    }
}
