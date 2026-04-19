package org.sokybot.party.coordination.internal;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.sokybot.party.coordination.api.IPartyCoordinator;
import org.sokybot.party.coordination.api.PartyMatchPosting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory broker for multi-bot invite coordination (same JVM). Pending invites expire after a TTL if unsettled.
 */
@Component(service = IPartyCoordinator.class, immediate = true)
public final class PartyCoordinatorImpl implements IPartyCoordinator {

    private static final Logger log = LoggerFactory.getLogger(PartyCoordinatorImpl.class);

    private static final long INVITE_EXPIRY_MS = 120_000L;

    private final ConcurrentHashMap<String, CompletableFuture<Boolean>> pendingByInvitee = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, PartyMatchPosting> matchingByLeader = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;

    @Activate
    void activate() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sokybot-party-coordinator");
            t.setDaemon(true);
            return t;
        });
        log.info("Party coordinator activated");
    }

    @Deactivate
    void deactivate() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
        }
        pendingByInvitee.clear();
        matchingByLeader.clear();
        log.info("Party coordinator deactivated");
    }

    private static String normalize(String machineId) {
        if (machineId == null) {
            return "";
        }
        return machineId.trim();
    }

    @Override
    public void requestInvite(String inviterMachine, String inviteeMachine) {
        Objects.requireNonNull(inviterMachine, "inviterMachine");
        Objects.requireNonNull(inviteeMachine, "inviteeMachine");
        String invitee = normalize(inviteeMachine);
        if (invitee.isEmpty()) {
            throw new IllegalArgumentException("inviteeMachine is empty");
        }
        CompletableFuture<Boolean> cf = new CompletableFuture<>();
        CompletableFuture<Boolean> previous = pendingByInvitee.put(invitee, cf);
        if (previous != null && !previous.isDone()) {
            previous.complete(Boolean.FALSE);
        }
        CompletableFuture<Boolean> tracked = cf;
        scheduler.schedule(() -> {
            CompletableFuture<Boolean> removed = pendingByInvitee.remove(invitee);
            if (removed == tracked && !tracked.isDone()) {
                tracked.complete(Boolean.FALSE);
            }
        }, INVITE_EXPIRY_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void registerMatchingPost(String leaderMachine, String title, int minLevel, int maxLevel) {
        String lm = normalize(Objects.requireNonNull(leaderMachine, "leaderMachine"));
        if (lm.isEmpty()) {
            throw new IllegalArgumentException("leaderMachine is empty");
        }
        matchingByLeader.put(lm, new PartyMatchPosting(lm, title != null ? title : "", minLevel, maxLevel));
    }

    @Override
    public boolean onInviteReceived(String inviteeMachine, int inviterEntityId) {
        String invitee = normalize(Objects.requireNonNull(inviteeMachine, "inviteeMachine"));
        if (invitee.isEmpty()) {
            return false;
        }
        if (log.isTraceEnabled()) {
            log.trace("onInviteReceived invitee={} inviterEntityId={}", invitee, inviterEntityId);
        }
        CompletableFuture<Boolean> cf = pendingByInvitee.remove(invitee);
        if (cf == null) {
            return false;
        }
        boolean accept = true;
        if (!cf.isDone()) {
            cf.complete(Boolean.valueOf(accept));
        }
        return accept;
    }
}
