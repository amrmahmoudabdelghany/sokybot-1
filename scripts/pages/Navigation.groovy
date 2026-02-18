import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler
import org.sokybot.machinepages.api.IScriptedPage
import org.sokybot.runtime.IMachineContext
import org.sokybot.settings.api.ISettingsRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

class NavigationPage implements IScriptedPage, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(NavigationPage.class)

    private IMachineContext machineContext
    private def trainingSettingsProvider
    
    private final Sinks.Many<Map<String, Object>> stateSink = Sinks.many().multicast().onBackpressureBuffer(100)

    @Override
    String getTitle() { "Navigation" }

    @Override
    String getIcon() { "Navigation" }

    @Override
    void initialize(IMachineContext context) {
        this.machineContext = context
        
        def settingsRegistry = context.getSokybotContext().getService(ISettingsRegistry.class)

        // Navigation settings are part of TrainingSettings
        this.trainingSettingsProvider = settingsRegistry.getProvider(
                context.getGroupName(),
                context.getMachineName(),
                "training",
                Object.class)

        this.trainingSettingsProvider.subscribe { settings -> emitStateUpdate() }
        
        log.info("Groovy NavigationPage initialized for {}", context.fullName())
    }

    @Override
    Map<String, Object> getSchema() {
        return [:] // Loaded from Navigation.json
    }

    @Override
    Map<String, Object> getInitialState() {
        return getState()
    }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        log.debug("Handling action: {} with data: {}", action, data)
        
        try {
            switch (action) {
                case "refresh":
                    break
                case "save":
                    trainingSettingsProvider.save()
                    break
                case "update":
                    updateSettings(data)
                    break
            }
            
            def newState = getState()
            newState.put("success", true)
            return newState
            
        } catch (Exception e) {
            log.error("Error handling action ${action}", e)
            return [success: false, error: e.message]
        }
    }

    @Override
    Flux<Object> streamData(String streamName, Map<String, Object> params) {
        return Flux.concat(
                Flux.just(getState()),
                stateSink.asFlux()
        )
    }

    @Override
    void handleEvent(Event event) {
        String topic = event.getTopic()
        if (topic.contains("PositionUpdate") || topic.contains("MapChanged")) {
            emitStateUpdate()
        }
    }

    @Override
    void shutdown() {
        stateSink.tryEmitComplete()
    }

    private void updateSettings(Map<String, Object> data) {
        trainingSettingsProvider.update { settings ->
            if (data.containsKey("loopInTown")) settings.loopInTown = data.get("loopInTown")
            if (data.containsKey("scriptPath")) settings.scriptPath = data.get("scriptPath")
        }
    }

    private Map<String, Object> getState() {
        def settings = trainingSettingsProvider.get()
        def trainer = machineContext.getGameModel().getTrainer()
        
        def position = [x: 0, y: 0]
        if (trainer != null) {
            position.x = trainer.getX()
            position.y = trainer.getY()
        }
        
        return [
            settings: settings,
            position: position
        ]
    }

    private void emitStateUpdate() {
        stateSink.tryEmitNext(getState())
    }
}

new NavigationPage()
