import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler
import org.sokybot.machinepages.api.IScriptedPage
import org.sokybot.runtime.IMachineContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

class SkillsPage implements IScriptedPage, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(SkillsPage.class)

    private IMachineContext machineContext
    private final Sinks.Many<Map<String, Object>> stateSink = Sinks.many().multicast().onBackpressureBuffer(100)
    private int skillPoints = 0

    @Override
    String getTitle() { "Skills" }

    @Override
    String getIcon() { "Zap" }

    @Override
    void initialize(IMachineContext context) {
        this.machineContext = context
        log.info("Groovy SkillsPage initialized for {}", context.fullName())
    }

    @Override
    Map<String, Object> getSchema() {
        return [:] // Loaded from Skills.json
    }

    @Override
    Map<String, Object> getInitialState() {
        return getSkillsData()
    }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        log.debug("Handling action: {} with data: {}", action, data)
        switch (action) {
            case "refresh":
                emitStateUpdate()
                break
        }
        def newState = getSkillsData()
        newState.put("success", true)
        return newState
    }

    @Override
    Flux<Object> streamData(String streamName, Map<String, Object> params) {
        return Flux.concat(
                Flux.just(getSkillsData()),
                stateSink.asFlux()
        )
    }

    @Override
    void handleEvent(Event event) {
        log.debug("Skill event received in Groovy: {}", event.getTopic())
        
        // Extract skill points if available
        def eventObj = event.getProperty("event")
        if (eventObj && eventObj.metaClass.respondsTo(eventObj, "getNewSkillPoints")) {
            this.skillPoints = eventObj.getNewSkillPoints()
        }
        
        emitStateUpdate()
    }

    @Override
    void shutdown() {
        stateSink.tryEmitComplete()
    }

    private Map<String, Object> getSkillsData() {
        def skills = []
        def trainer = machineContext.getGameModel().getTrainer()
        if (trainer != null && trainer.getSkills() != null) {
            trainer.getSkills().each { skill ->
                if (skill != null) {
                    skills << [
                        refId: String.valueOf(skill.getRefId()),
                        name: skill.getName(),
                        level: skill.getSkillLvl(),
                        enabled: skill.isEnabled()
                    ]
                }
            }
            skills.sort { a, b -> Long.parseLong(a.refId) <=> Long.parseLong(b.refId) }
        }
        return [
            skills: skills,
            skillPoints: this.skillPoints
        ]
    }

    private void emitStateUpdate() {
        stateSink.tryEmitNext(getSkillsData())
    }
}

new SkillsPage()
