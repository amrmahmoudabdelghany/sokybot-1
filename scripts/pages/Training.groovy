import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler
import org.sokybot.machinepages.api.IScriptedPage
import org.sokybot.runtime.IMachineContext
import org.sokybot.settings.api.ISettingsRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

class TrainingPage implements IScriptedPage, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(TrainingPage.class)

    private IMachineContext machineContext
    private def trainingSettingsProvider
    
    private final Sinks.Many<Map<String, Object>> stateSink = Sinks.many().multicast().onBackpressureBuffer(100)

    @Override
    String getTitle() { "Training" }

    @Override
    String getIcon() { "Target" }

    @Override
    void initialize(IMachineContext context) {
        this.machineContext = context
        
        def settingsRegistry = context.getSokybotContext().getService(ISettingsRegistry.class)

        // Access TrainingSettings from the registry.
        this.trainingSettingsProvider = settingsRegistry.getProvider(
                context.getGroupName(),
                context.getMachineName(),
                "training",
                Object.class)

        this.trainingSettingsProvider.subscribe { settings -> emitStateUpdate() }
        
        log.info("Groovy TrainingPage initialized for {}", context.fullName())
    }

    @Override
    Map<String, Object> getSchema() {
        return [:] // Loaded from Training.json by ScriptPageLoader
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
        // Handle TrainerStuckEvent or other training related events
        String topic = event.getTopic()
        if (topic.contains("TrainerStuckEvent")) {
            log.debug("Trainer stuck event detected in Groovy UI")
            emitStateUpdate()
        }
    }

    @Override
    void shutdown() {
        stateSink.tryEmitComplete()
    }

    private void updateSettings(Map<String, Object> data) {
        trainingSettingsProvider.update { settings ->
            if (data.containsKey("autoAttack")) settings.autoAttack = data.get("autoAttack")
            if (data.containsKey("doNotAttack")) settings.doNotAttack = data.get("doNotAttack")
            if (data.containsKey("areaX")) settings.areaX = data.get("areaX")
            if (data.containsKey("areaY")) settings.areaY = data.get("areaY")
            if (data.containsKey("areaRadius")) settings.areaRadius = data.get("areaRadius")
            if (data.containsKey("activeAreaName")) settings.activeAreaName = data.get("activeAreaName")
        }
    }

    private Map<String, Object> getState() {
        def settings = trainingSettingsProvider.get()
        
        def activeArea = null
        if (settings.activeAreaName || settings.areaRadius > 0) {
            activeArea = [
                name: settings.activeAreaName ?: "Custom Area",
                x: settings.areaX,
                y: settings.areaY,
                radius: settings.areaRadius
            ]
        }
        
        return [
            settings: settings,
            isDirty: trainingSettingsProvider.isDirty(),
            activeArea: activeArea
        ]
    }

    private void emitStateUpdate() {
        stateSink.tryEmitNext(getState())
    }
}

new TrainingPage()
