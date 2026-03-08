import org.sokybot.machinepages.api.BasePage
import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler

class LogPage extends BasePage implements EventHandler {

    private final List<Map<String, Object>> eventBuffer = []
    private int maxEvents = 100
    private String level = "ALL"
    private String feature = "ALL"

    LogPage() { super("Log", "FileText") }

    @Override
    String[] getEventTopics(String machineFullName) {
        ["sokybot/game/${machineFullName}/*",
         "sokybot/network/${machineFullName}/*"] as String[]
    }

    @Override
    Map<String, Object> getInitialState() { return getLogData() }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        switch (action) {
            case "clearLog":
                synchronized (eventBuffer) { eventBuffer.clear() }
                emitStateUpdate()
                return [success: true]
            case "setMaxEvents":
                this.maxEvents = ((Number) data.getOrDefault("value", 100)).intValue()
                synchronized (eventBuffer) {
                    while (eventBuffer.size() > maxEvents) {
                        eventBuffer.remove(eventBuffer.size() - 1)
                    }
                }
                emitStateUpdate()
                return [success: true]
            case "setLogLevel":
                this.level = data?.get("value")?.toString() ?: "ALL"
                return [Log: [level: this.level, feature: this.feature], success: true]
            case "setLogFeature":
                this.feature = data?.get("value")?.toString() ?: "ALL"
                return [Log: [level: this.level, feature: this.feature], success: true]
        }
        return [success: false, error: "Unknown action: ${action}"]
    }

    @Override
    void handleEvent(Event event) {
        Map<String, Object> logEntry = [
            eventType: event.getProperty("eventType"),
            timestamp: event.getProperty("timestamp") ?: System.currentTimeMillis(),
            event: event.getProperty("event")?.toString() ?: "No details"
        ]
        synchronized (eventBuffer) {
            eventBuffer.add(0, logEntry)
            while (eventBuffer.size() > maxEvents) {
                eventBuffer.remove(eventBuffer.size() - 1)
            }
        }
        emitStateUpdate()
    }

    private Map<String, Object> getLogData() {
        synchronized (eventBuffer) {
            return [events: new ArrayList(eventBuffer), maxEvents: this.maxEvents]
        }
    }
}

new LogPage()
