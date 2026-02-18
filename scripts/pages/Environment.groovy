import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler
import org.sokybot.machinepages.api.IScriptedPage
import org.sokybot.runtime.IMachineContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

class EnvironmentPage implements IScriptedPage, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentPage.class)

    private IMachineContext machineContext
    private final Sinks.Many<Map<String, Object>> stateSink = Sinks.many().multicast().onBackpressureBuffer(100)

    @Override
    String getTitle() { "Environment" }

    @Override
    String getIcon() { "Globe" }

    @Override
    void initialize(IMachineContext context) {
        this.machineContext = context
        log.info("Groovy EnvironmentPage initialized for {}", context.fullName())
    }

    @Override
    Map<String, Object> getSchema() {
        return [:] // Loaded from Environment.json
    }

    @Override
    Map<String, Object> getInitialState() {
        return getEnvironmentData()
    }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        log.debug("Handling action: {} with data: {}", action, data)
        switch (action) {
            case "refresh":
                emitStateUpdate()
                break
        }
        def newState = getEnvironmentData()
        newState.put("success", true)
        return newState
    }

    @Override
    Flux<Object> streamData(String streamName, Map<String, Object> params) {
        return Flux.concat(
                Flux.just(getEnvironmentData()),
                stateSink.asFlux()
        )
    }

    @Override
    void handleEvent(Event event) {
        log.debug("Environment event received in Groovy: {}", event.getTopic())
        emitStateUpdate()
    }

    @Override
    void shutdown() {
        stateSink.tryEmitComplete()
    }

    private Map<String, Object> getEnvironmentData() {
        return [
            monsterPreferences: [:],
            areaFilters: [:]
        ]
    }

    private void emitStateUpdate() {
        stateSink.tryEmitNext(getEnvironmentData())
    }
}

new EnvironmentPage()
