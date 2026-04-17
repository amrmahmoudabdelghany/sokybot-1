import org.sokybot.machinepages.api.BasePage
import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler
import org.sokybot.runtime.ISokybotContext

class PartyPage extends BasePage implements EventHandler {

    def partySettings
    private final Map<String, Map<String, Object>> members = [:]

    PartyPage() { super("Party", "Users") }

    @Override
    String[] getEventTopics(String machineFullName) {
        def mid = osgiEventTopicSegment(machineFullName)
        [
                "sokybot/game/${mid}/PartyInviteEvent",
                "sokybot/game/${mid}/PartyUpdateEvent",
                "sokybot/party/*"
        ] as String[]
    }

    @Override
    void setup() {
        partySettings = settingsProvider("party", Object)
        partySettings?.subscribe { emitStateUpdate() }
    }

    @Override
    Map<String, Object> getInitialState() {
        def settings = partySettings?.get() ?: [:]
        return [
                settings      : settings,
                isDirty       : partySettings?.isDirty() ?: false,
                members       : members.values().toList().sort { a, b -> String.valueOf(a.name).compareTo(String.valueOf(b.name)) },
                leaderOptions : resolveLeaderOptions(),
                machineId     : machineContext?.fullName(),
                groupName     : machineContext?.getGroupName()
        ]
    }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        try {
            switch (action) {
                case "refresh":
                    break
                case "save":
                    partySettings?.save()
                    break
                case "update":
                    applyFrom(partySettings, data, ["partyAutoInvite", "partyAutoAccept", "partyLeaderMachineId"])
                    break
                case "clearMembers":
                    members.clear()
                    break
                default:
                    break
            }
            return withSuccess(getInitialState())
        } catch (Exception e) {
            log.error("Party page action error ${action}", e)
            return withError(e.message)
        }
    }

    @Override
    void handleEvent(Event event) {
        try {
            if (event == null) return
            String topic = String.valueOf(event.getTopic() ?: "")
            if (topic.contains("PartyUpdateEvent")) {
                upsertMemberFromUpdate(event)
                emitStateUpdate()
                return
            }
            if (topic.contains("PartyInviteEvent")) {
                emitStateUpdate()
                return
            }
            if (topic.startsWith("sokybot/party/")) {
                emitStateUpdate()
            }
        } catch (Exception ignored) {
        }
    }

    private void upsertMemberFromUpdate(Event event) {
        String memberName = readString(event, "event.memberName")
        if (memberName == null || memberName.trim().isEmpty()) {
            memberName = readString(event, "memberName")
        }
        Integer memberId = readInt(event, "event.memberUniqueId")
        if (memberId == null) memberId = readInt(event, "memberUniqueId")
        Integer memberLevel = readInt(event, "event.memberLevel")
        if (memberLevel == null) memberLevel = readInt(event, "memberLevel")
        Integer healthMana = readInt(event, "event.memberHealthMana")
        if (healthMana == null) healthMana = readInt(event, "memberHealthMana")

        if (memberName == null || memberName.trim().isEmpty()) {
            if (memberId == null) return
            memberName = "Member-${memberId}"
        }
        String key = String.valueOf(memberId ?: memberName)
        members.put(key, [
                id        : memberId,
                name      : memberName,
                level     : memberLevel,
                healthMana: healthMana
        ])
    }

    private List<String> resolveLeaderOptions() {
        def ctx = service(ISokybotContext)
        if (ctx == null) return []
        try {
            def group = ctx.findGroupCtx(machineContext?.getGroupName()).orElse(null)
            if (group == null) return []
            return group.getMachines()
                    .collect { it.fullName() }
                    .sort()
        } catch (Exception ignored) {
            return []
        }
    }

    private String readString(Event event, String key) {
        try {
            def value = event.getProperty(key)
            if (value == null) return null
            return String.valueOf(value)
        } catch (Exception ignored) {
            return null
        }
    }

    private Integer readInt(Event event, String key) {
        try {
            def value = event.getProperty(key)
            if (value instanceof Number) return ((Number) value).intValue()
            if (value != null) return Integer.parseInt(String.valueOf(value))
            return null
        } catch (Exception ignored) {
            return null
        }
    }
}

new PartyPage()

