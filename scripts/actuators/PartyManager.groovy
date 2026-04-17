import org.osgi.service.event.Event
import org.osgi.service.event.EventAdmin
import org.sokybot.http.server.events.IEventBridge
import org.sokybot.runtime.ISokybotContext

class PartyManager extends BaseActuator {

    private static final String SCOPE = "party"
    private static final String CYCLE_NAME = "party-cycle"

    private static final String STATE_IDLE = "IDLE"
    private static final String STATE_LEADER_INVITING = "LEADER_INVITING"
    private static final String STATE_MEMBER_AWAITING_INVITE = "MEMBER_AWAITING_INVITE"
    private static final String STATE_IN_PARTY = "IN_PARTY"

    private static final String SESSION_SUB_KEY = "party.bridge.sub"
    private static final String SESSION_INVITE_AT = "party.invite.intent.at"
    private static final String SESSION_PARTY_ACTIVE = "party.active"
    private static final String SESSION_LAST_INVITE_BROADCAST_MS = "party.last.invite.broadcast.ms"
    private static final long INVITE_BROADCAST_INTERVAL_MS = 5000L

    PartyManager() { super("party") }

    @Override
    void setup() {
        registerSettings(SCOPE, PartySettings, { new PartySettings() })
        def settingsProvider = settingsProvider(SCOPE, PartySettings)
        if (settingsProvider == null) {
            log.warn("PartyManager: settings provider not available; cycle not registered")
            return
        }

        attachBridgeSubscription()

        def cycle = new CycleDefinitionBuilder()
                .name(CYCLE_NAME)
                .priority(350)
                .entryState(STATE_IDLE)
                .entryGuard({ ctx -> isLoggedIn(ctx) })

                .state(STATE_IDLE, { builder -> builder
                        .guard({ ctx -> shouldRunAsLeader(ctx, settingsProvider?.get()) })
                        .action({ ctx -> markPartyInactiveIfStale(ctx) })
                        .nextState(STATE_LEADER_INVITING)
                        .targetState(STATE_MEMBER_AWAITING_INVITE)
                })
                .state(STATE_MEMBER_AWAITING_INVITE, { builder -> builder
                        .guard({ ctx -> shouldRunAsMember(ctx, settingsProvider?.get()) && hasInviteIntent(ctx) })
                        .action({ ctx -> publishAcceptIntent(ctx, settingsProvider?.get()) })
                        .nextState(STATE_IN_PARTY)
                        .targetState(STATE_IDLE)
                })
                .state(STATE_LEADER_INVITING, { builder -> builder
                        .guard({ ctx -> shouldRunAsLeader(ctx, settingsProvider?.get()) })
                        .action({ ctx -> publishInviteIntents(ctx, settingsProvider?.get()) })
                        .nextState(STATE_IN_PARTY)
                        .targetState(STATE_IDLE)
                })
                .state(STATE_IN_PARTY, { builder -> builder
                        .guard({ ctx -> isPartyActive(ctx) })
                        .action({ ctx -> publishHeartbeat(ctx, settingsProvider?.get()) })
                        .nextState(STATE_IN_PARTY)
                        .targetState(STATE_IDLE)
                })
                .delayState("PARTY_TICK", { builder -> builder.delay(1000).nextState(STATE_IDLE) })
                .build()

        context.getWorkflowRegistry().registerCycle(cycle)
        log.info("PartyManager cycle registered successfully")
    }

    private void attachBridgeSubscription() {
        if (context.getSessionData().get(SESSION_SUB_KEY) != null) {
            return
        }
        def bridge = context.getService(IEventBridge)
        if (bridge == null) {
            log.warn("PartyManager: IEventBridge unavailable; falling back to local-only behavior")
            return
        }
        String groupSeg = topicSegment(context.getGroupName())
        def sub = bridge.subscribe("sokybot.party.${groupSeg}.**", { bridgeEvent ->
            try {
                def payload = bridgeEvent?.getPayload()
                if (!(payload instanceof Map)) return
                String targetMachineId = String.valueOf(payload.targetMachineId ?: "")
                if (!targetMachineId.isEmpty() && targetMachineId != context.getMachineId()) return
                String eventType = String.valueOf(payload.eventType ?: "")
                if ("invite_intent".equals(eventType)) {
                    context.getSessionData().put(SESSION_INVITE_AT, System.currentTimeMillis())
                } else if ("party_active".equals(eventType) || "accept_intent".equals(eventType)) {
                    context.getSessionData().put(SESSION_PARTY_ACTIVE, true)
                } else if ("party_inactive".equals(eventType)) {
                    context.getSessionData().put(SESSION_PARTY_ACTIVE, false)
                }
            } catch (Exception ignored) {
            }
        })
        context.getSessionData().put(SESSION_SUB_KEY, sub)
    }

    private boolean isLoggedIn(def ctx) {
        try {
            def trainer = ctx?.getGameModel()?.getTrainer()
            return trainer != null && trainer.getUniqueId() > 0
        } catch (Exception ignored) {
            return false
        }
    }

    private boolean shouldRunAsLeader(def ctx, PartySettings settings) {
        if (settings == null || !settings.partyAutoInvite) return false
        return isLeaderMachine(settings)
    }

    private boolean shouldRunAsMember(def ctx, PartySettings settings) {
        if (settings == null || !settings.partyAutoAccept) return false
        return !isLeaderMachine(settings)
    }

    private boolean isLeaderMachine(PartySettings settings) {
        String configured = String.valueOf(settings?.partyLeaderMachineId ?: "").trim()
        if (configured.isEmpty()) return false
        return configured == context.getMachineId()
    }

    private boolean hasInviteIntent(def ctx) {
        def ts = ctx?.getSessionData()?.get(SESSION_INVITE_AT)
        if (!(ts instanceof Number)) return false
        return (System.currentTimeMillis() - ((Number) ts).longValue()) <= 15000L
    }

    private boolean isPartyActive(def ctx) {
        def active = ctx?.getSessionData()?.get(SESSION_PARTY_ACTIVE)
        return Boolean.TRUE.equals(active)
    }

    private void markPartyInactiveIfStale(def ctx) {
        if (!hasInviteIntent(ctx)) {
            ctx.getSessionData().put(SESSION_PARTY_ACTIVE, false)
        }
    }

    private void publishInviteIntents(def ctx, PartySettings settings) {
        long now = System.currentTimeMillis()
        def lastRaw = ctx.getSessionData().get(SESSION_LAST_INVITE_BROADCAST_MS)
        long last = (lastRaw instanceof Number) ? ((Number) lastRaw).longValue() : 0L
        if (now - last < INVITE_BROADCAST_INTERVAL_MS) {
            return
        }
        def members = resolveGroupMachineIds(ctx.getGroupName()).findAll { it != context.getMachineId() }
        members.each { target ->
            postPartyCoordinationEvent(ctx, ctx.getGroupName(), "invite", [
                    eventType      : "invite_intent",
                    leaderMachineId: context.getMachineId(),
                    targetMachineId: target
            ])
        }
        ctx.getSessionData().put(SESSION_LAST_INVITE_BROADCAST_MS, now)
    }

    private void publishAcceptIntent(def ctx, PartySettings settings) {
        postPartyCoordinationEvent(ctx, ctx.getGroupName(), "accept", [
                eventType      : "accept_intent",
                leaderMachineId: String.valueOf(settings?.partyLeaderMachineId ?: ""),
                targetMachineId: context.getMachineId()
        ])
        ctx.getSessionData().put(SESSION_PARTY_ACTIVE, true)
    }

    private void publishHeartbeat(def ctx, PartySettings settings) {
        postPartyCoordinationEvent(ctx, ctx.getGroupName(), "status", [
                eventType      : "party_active",
                leaderMachineId: String.valueOf(settings?.partyLeaderMachineId ?: ""),
                targetMachineId: context.getMachineId()
        ])
    }

    private void postPartyCoordinationEvent(def ctx, String groupName, String eventType, Map<String, Object> extras) {
        def eventAdmin = context?.getService(EventAdmin)
        if (eventAdmin == null) return
        String topic = "sokybot/party/${topicSegment(groupName)}/${topicSegment(eventType)}"
        def props = [
                machineId   : context.getMachineId(),
                groupName   : groupName,
                eventType   : String.valueOf(extras?.eventType ?: eventType),
                timestamp   : System.currentTimeMillis()
        ] as Map<String, Object>
        if (extras != null) props.putAll(extras)
        eventAdmin.postEvent(new Event(topic, props))
    }

    private List<String> resolveGroupMachineIds(String groupName) {
        def sokybotContext = context.getService(ISokybotContext)
        if (sokybotContext == null) return []
        try {
            def group = sokybotContext.findGroupCtx(groupName).orElse(null)
            if (group == null) return []
            return group.getMachines().collect { it.fullName() }
        } catch (Exception ignored) {
            return []
        }
    }

    private String topicSegment(String value) {
        if (value == null) return "_"
        String s = String.valueOf(value).trim()
        if (s.isEmpty()) return "_"
        return s.replaceAll("[^A-Za-z0-9._-]", "_")
    }
}

class PartySettings {
    boolean partyAutoInvite = false
    boolean partyAutoAccept = true
    String partyLeaderMachineId = ""
}

new PartyManager()

