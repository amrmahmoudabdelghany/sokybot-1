import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler
import org.sokybot.machinepages.api.IScriptedPage
import org.sokybot.runtime.IMachineContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

class LogPage implements IScriptedPage, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(LogPage.class)

    private IMachineContext machineContext
    // Legacy event buffer kept for compatibility with older runtimes.
    // When the central logging service is active, the machine log page
    // state is built from ILogStreamService instead; this buffer is then
    // only used for the "clear" action on older setups.
    private final List<Map<String, Object>> eventBuffer = []
    private final Sinks.Many<Map<String, Object>> stateSink = Sinks.many().multicast().onBackpressureBuffer(1000)
    private int maxEvents = 100
    private String level = "ALL"
    private String feature = "ALL"

     
    String getTitle() { "Log" }

     
    String getIcon() { "FileText" }

     
     @Override void init(IMachineContext context) {
        this.machineContext = context
        log.info("Groovy LogPage initialized for {}", context.fullName())
    }

     
    Map<String, Object> getSchema() {
        return [:] // Loaded from Log.json
    }

     
    Map<String, Object> getInitialState() {
        return getLogData()
    }

     
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        log.debug("Handling action: {} with data: {}", action, data)
        switch (action) {
            case "clearLog":
                synchronized (eventBuffer) {
                    eventBuffer.clear()
                }
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
                def value = data?.get("value") ?: "ALL"
                this.level = value?.toString() ?: "ALL"
                return [
                    Log: [
                        level  : this.level,
                        feature: this.feature
                    ],
                    success: true
                ]
            case "setLogFeature":
                def value = data?.get("value") ?: "ALL"
                this.feature = value?.toString() ?: "ALL"
                return [
                    Log: [
                        level  : this.level,
                        feature: this.feature
                    ],
                    success: true
                ]
        }
        return [success: false, error: "Unknown action: ${action}"]
    }

     
    Flux<Object> streamData(String streamName, Map<String, Object> params) {
        return Flux.concat(
                Flux.just(getLogData()),
                stateSink.asFlux()
        )
    }

     
    void handleEvent(Event event) {
        // Log all machine-specific events
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

     
    void shutdown() {
        stateSink.tryEmitComplete()
    }

    private Map<String, Object> getLogData() {
        synchronized (eventBuffer) {
            return [
                events: new ArrayList(eventBuffer),
                maxEvents: this.maxEvents
            ]
        }
    }

    private void emitStateUpdate() {
        stateSink.tryEmitNext(getLogData())
    }
}

new LogPage()
