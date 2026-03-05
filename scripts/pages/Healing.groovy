import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler
import org.sokybot.machinepages.api.IScriptedPage
import org.sokybot.runtime.IMachineContext
import org.sokybot.settings.api.ISettingsRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

class HealingPage implements IScriptedPage, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(HealingPage.class)

    private IMachineContext machineContext
    private def trainingSettingsProvider
    
    private final Sinks.Many<Map<String, Object>> stateSink = Sinks.many().multicast().onBackpressureBuffer(100)

     
    String getTitle() { "Healing" }

     
    String getIcon() { "Heart" }

     
     @Override void init(IMachineContext context) {
        this.machineContext = context
        
        def settingsRegistry = context.getSokybotContext().getService(ISettingsRegistry.class)

        // Healing settings are part of TrainingSettings
        this.trainingSettingsProvider = settingsRegistry.getProvider(
                context.getGroupName(),
                context.getMachineName(),
                "training",
                Object.class)

        this.trainingSettingsProvider.subscribe { settings -> emitStateUpdate() }
        
        log.info("Groovy HealingPage initialized for {}", context.fullName())
    }

     
    Map<String, Object> getSchema() {
        return [:] // Loaded from Healing.json
    }

     
    Map<String, Object> getInitialState() {
        return getState()
    }

     
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

     
    Flux<Object> streamData(String streamName, Map<String, Object> params) {
        return Flux.concat(
                Flux.just(getState()),
                stateSink.asFlux()
        )
    }

     
    void handleEvent(Event event) {
        String topic = event.getTopic()
        if (topic.contains("UpdateHP") || topic.contains("UpdateMP")) {
            emitStateUpdate()
        }
    }

     
    void shutdown() {
        stateSink.tryEmitComplete()
    }

    private void updateSettings(Map<String, Object> data) {
        trainingSettingsProvider.update { settings ->
            if (data.containsKey("hpPotionThreshold")) settings.hpPotionThreshold = data.get("hpPotionThreshold")
            if (data.containsKey("mpPotionThreshold")) settings.mpPotionThreshold = data.get("mpPotionThreshold")
            if (data.containsKey("useHpPotion")) settings.useHpPotion = data.get("useHpPotion")
            if (data.containsKey("useMpPotion")) settings.useMpPotion = data.get("useMpPotion")
        }
    }

    private Map<String, Object> getState() {
        def settings = trainingSettingsProvider.get()
        def trainer = machineContext.getGameModel().getTrainer()
        
        def currentHP = 0, maxHP = 100, currentMP = 0, maxMP = 100
        if (trainer != null) {
            currentHP = trainer.getCurrentHP()
            maxHP = trainer.getMaxHP() ?: 100
            currentMP = trainer.getCurrentMP()
            maxMP = trainer.getMaxMP() ?: 100
        }
        
        return [
            settings: settings,
            isDirty: trainingSettingsProvider.isDirty(),
            currentHP: currentHP,
            maxHP: maxHP,
            currentMP: currentMP,
            maxMP: maxMP
        ]
    }

    private void emitStateUpdate() {
        stateSink.tryEmitNext(getState())
    }
}

new HealingPage()
