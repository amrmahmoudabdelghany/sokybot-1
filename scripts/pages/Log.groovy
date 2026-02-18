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
    private final List<Map<String, Object>> eventBuffer = []
    private final Sinks.Many<Map<String, Object>> stateSink = Sinks.many().multicast().onBackpressureBuffer(1000)
    private int maxEvents = 100

    @Override
    String getTitle() { "Log" }

    @Override
    String getIcon() { "FileText" }

    @Override
    void initialize(IMachineContext context) {
        this.machineContext = context
        log.info("Groovy LogPage initialized for {}", context.fullName())
    }

    @Override
    Map<String, Object> getSchema() {
        return [:] // Loaded from Log.json
    }

    @Override
    Map<String, Object> getInitialState() {
        return getLogData()
    }

    @Override
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
        }
        return [success: false, error: "Unknown action: ${action}"]
    }

    @Override
    Flux<Object> streamData(String streamName, Map<String, Object> params) {
        return Flux.concat(
                Flux.just(getLogData()),
                stateSink.asFlux()
        )
    }

    @Override
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

    @Override
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
