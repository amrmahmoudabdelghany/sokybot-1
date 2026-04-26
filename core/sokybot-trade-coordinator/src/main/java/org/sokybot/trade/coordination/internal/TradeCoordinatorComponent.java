package org.sokybot.trade.coordination.internal;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.engine.api.EngineEvent;
import org.sokybot.engine.api.handler.IEngineEventMediator;
import org.sokybot.trade.coordination.api.FarmerProfile;
import org.sokybot.trade.coordination.api.ITradeCoordinator;
import org.sokybot.trade.coordination.api.MuleHandle;
import org.sokybot.trade.coordination.api.MuleIntent;
import org.sokybot.trade.coordination.api.SwarmRole;
import org.sokybot.trade.coordination.api.SwarmSessionPhase;
import org.sokybot.trade.coordination.api.SwarmSessionResult;
import org.sokybot.trade.coordination.api.SwarmSessionState;
import org.sokybot.trade.coordination.api.TradeOutcome;
import org.sokybot.trade.coordination.event.MuleAvailable;
import org.sokybot.trade.coordination.event.MuleUnavailable;
import org.sokybot.trade.coordination.event.StallReady;
import org.sokybot.trade.coordination.event.TradeCompleted;
import org.sokybot.trade.coordination.event.TradeOfferAccepted;
import org.sokybot.trade.coordination.event.TradeOfferProposed;
import org.sokybot.trade.coordination.event.TradeOfferRejected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory coordinator; relays all coordination signals through the process-wide {@link IEngineEventMediator}.
 */
@Component(service = ITradeCoordinator.class, immediate = true)
public final class TradeCoordinatorComponent implements ITradeCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TradeCoordinatorComponent.class);

    private volatile IEngineEventMediator mediator;

    private final Map<String, MuleRegistration> mulesByMachineId = new ConcurrentHashMap<>();
    private final Map<String, TradeSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, SwarmSessionState> swarmSessionsByLocalMachineId = new ConcurrentHashMap<>();

    @Activate
    void activate() {
        log.info("Trade coordinator activated");
    }

    @Deactivate
    void deactivate() {
        mulesByMachineId.clear();
        sessionsById.clear();
        swarmSessionsByLocalMachineId.clear();
        log.info("Trade coordinator deactivated");
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void bindMediator(IEngineEventMediator m) {
        this.mediator = m;
        log.debug("IEngineEventMediator bound");
    }

    protected void unbindMediator(IEngineEventMediator m) {
        if (this.mediator == m) {
            this.mediator = null;
            log.debug("IEngineEventMediator unbound");
        }
    }

    private void relay(EngineEvent event) {
        IEngineEventMediator m = mediator;
        if (m == null) {
            log.warn("No IEngineEventMediator; dropping coordination event: {}", event.type());
            return;
        }
        m.relay(event);
    }

    @Override
    public String registerMule(MuleIntent intent) {
        Objects.requireNonNull(intent, "intent");
        String ticketId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        MuleRegistration reg = new MuleRegistration(ticketId, intent.getMachineId(),
                intent.getFreeCargoSlots(), intent.getLocationHint(), now);
        mulesByMachineId.put(intent.getMachineId(), reg);

        relay(new MuleAvailable(intent.getMachineId(), ticketId, intent.getFreeCargoSlots(),
                intent.getLocationHint()));
        return ticketId;
    }

    @Override
    public void unregisterMule(String machineId) {
        Objects.requireNonNull(machineId, "machineId");
        if (mulesByMachineId.remove(machineId.trim()) != null) {
            relay(new MuleUnavailable(machineId.trim()));
        }
    }

    @Override
    public Optional<MuleHandle> findBestMule(FarmerProfile profile) {
        Objects.requireNonNull(profile, "profile");
        String farmer = profile.getFarmerMachineId();
        return mulesByMachineId.values().stream()
                .filter(r -> !r.machineId.equals(farmer))
                .filter(r -> r.freeCargoSlots > 0)
                .min(Comparator.comparingLong(r -> r.registeredAtEpochMillis))
                .map(r -> new MuleHandle(r.ticketId, r.machineId, r.freeCargoSlots, r.locationHint,
                        r.registeredAtEpochMillis));
    }

    @Override
    public String openTradeSession(String farmerMachineId, String muleMachineId) {
        String farmer = Objects.requireNonNull(farmerMachineId, "farmerMachineId").trim();
        String mule = Objects.requireNonNull(muleMachineId, "muleMachineId").trim();
        if (farmer.isEmpty() || mule.isEmpty()) {
            throw new IllegalArgumentException("machine ids cannot be empty");
        }
        if (farmer.equals(mule)) {
            throw new IllegalArgumentException("farmer and mule must differ");
        }
        if (!mulesByMachineId.containsKey(mule)) {
            throw new IllegalStateException("Mule is not registered: " + mule);
        }

        String sessionId = UUID.randomUUID().toString();
        sessionsById.put(sessionId, new TradeSession(farmer, mule));
        relay(new TradeOfferProposed(sessionId, farmer, mule));
        return sessionId;
    }

    @Override
    public void acceptTradeSession(String sessionId, String respondingMachineId) {
        TradeSession session = requireSession(sessionId);
        String responder = Objects.requireNonNull(respondingMachineId, "respondingMachineId").trim();
        if (!responder.equals(session.farmerMachineId) && !responder.equals(session.muleMachineId)) {
            throw new IllegalArgumentException("respondingMachineId does not belong to session");
        }
        relay(new TradeOfferAccepted(sessionId, responder));
    }

    @Override
    public void rejectTradeSession(String sessionId, String respondingMachineId, String reason) {
        TradeSession session = sessionsById.remove(Objects.requireNonNull(sessionId, "sessionId"));
        if (session == null) {
            throw new IllegalArgumentException("Unknown session: " + sessionId);
        }
        String responder = Objects.requireNonNull(respondingMachineId, "respondingMachineId").trim();
        if (!responder.equals(session.farmerMachineId) && !responder.equals(session.muleMachineId)) {
            throw new IllegalArgumentException("respondingMachineId does not belong to session");
        }
        relay(new TradeOfferRejected(sessionId, responder, reason != null ? reason : ""));
    }

    @Override
    public void completeTradeSession(String sessionId, TradeOutcome outcome) {
        Objects.requireNonNull(outcome, "outcome");
        TradeSession session = sessionsById.remove(Objects.requireNonNull(sessionId, "sessionId"));
        if (session == null) {
            throw new IllegalArgumentException("Unknown session: " + sessionId);
        }
        relay(new TradeCompleted(sessionId, outcome, session.farmerMachineId, session.muleMachineId));
    }

    @Override
    public void publishStallReady(String machineId, String stallId) {
        relay(new StallReady(
                Objects.requireNonNull(machineId, "machineId").trim(),
                Objects.requireNonNull(stallId, "stallId").trim()));
    }

    @Override
    public boolean openSwarmSession(String localMachineId, String partnerMachineId, String requestId, SwarmRole localRole) {
        Objects.requireNonNull(localRole, "localRole");
        String local = Objects.requireNonNull(localMachineId, "localMachineId").trim();
        String partner = Objects.requireNonNull(partnerMachineId, "partnerMachineId").trim();
        String rid = Objects.requireNonNull(requestId, "requestId").trim();
        if (local.isEmpty() || partner.isEmpty() || rid.isEmpty()) {
            return false;
        }
        if (local.equals(partner)) {
            return false;
        }
        long now = System.currentTimeMillis();
        SwarmSessionState created = new SwarmSessionState(rid, partner, SwarmSessionPhase.FILLING, now, localRole);
        SwarmSessionState prev = swarmSessionsByLocalMachineId.putIfAbsent(local, created);
        return prev == null;
    }

    @Override
    public void updateSwarmSessionPhase(String localMachineId, SwarmSessionPhase phase) {
        Objects.requireNonNull(phase, "phase");
        if (localMachineId == null) {
            return;
        }
        String local = localMachineId.trim();
        swarmSessionsByLocalMachineId.computeIfPresent(local, (k, s) -> s.withPhase(phase));
    }

    @Override
    public Optional<SwarmSessionState> getSwarmSession(String localMachineId) {
        if (localMachineId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(swarmSessionsByLocalMachineId.get(localMachineId.trim()));
    }

    @Override
    public void closeSwarmSession(String localMachineId, SwarmSessionResult result) {
        Objects.requireNonNull(result, "result");
        if (localMachineId == null) {
            return;
        }
        swarmSessionsByLocalMachineId.remove(localMachineId.trim());
    }

    private TradeSession requireSession(String sessionId) {
        TradeSession session = sessionsById.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Unknown session: " + sessionId);
        }
        return session;
    }

    private static final class MuleRegistration {
        final String ticketId;
        final String machineId;
        final int freeCargoSlots;
        final String locationHint;
        final long registeredAtEpochMillis;

        MuleRegistration(String ticketId, String machineId, int freeCargoSlots,
                String locationHint, long registeredAtEpochMillis) {
            this.ticketId = ticketId;
            this.machineId = machineId;
            this.freeCargoSlots = freeCargoSlots;
            this.locationHint = locationHint != null ? locationHint : "";
            this.registeredAtEpochMillis = registeredAtEpochMillis;
        }
    }

    private static final class TradeSession {
        final String farmerMachineId;
        final String muleMachineId;

        TradeSession(String farmerMachineId, String muleMachineId) {
            this.farmerMachineId = farmerMachineId;
            this.muleMachineId = muleMachineId;
        }
    }
}
