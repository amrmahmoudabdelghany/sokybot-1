import org.sokybot.machinepages.api.BasePage
import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler

class SkillsPage extends BasePage implements EventHandler {

    private int skillPoints = 0

    SkillsPage() { super("Skills", "Zap") }

    @Override
    String[] getEventTopics(String machineFullName) {
        ["sokybot/game/${machineFullName}/SkillUpdate",
         "sokybot/game/${machineFullName}/SkillPointsUpdate"] as String[]
    }

    @Override
    Map<String, Object> getInitialState() {
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

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        if (action == "refresh") emitStateUpdate()
        return withSuccess(getInitialState())
    }

    @Override
    void handleEvent(Event event) {
        def eventObj = event.getProperty("event")
        if (eventObj && eventObj.metaClass.respondsTo(eventObj, "getNewSkillPoints")) {
            this.skillPoints = eventObj.getNewSkillPoints()
        }
        emitStateUpdate()
    }
}

new SkillsPage()
