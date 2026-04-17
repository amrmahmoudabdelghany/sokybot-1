import org.sokybot.machinepages.api.BasePage
import org.sokybot.commons.topic.Topics
import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler

class HealingPage extends BasePage implements EventHandler {

    def trainingSettings

    HealingPage() { super("Healing", "Heart") }

    @Override
    String[] getEventTopics(String machineFullName) {
        def mid = osgiEventTopicSegment(machineFullName)
        [Topics.game(mid, "UpdateHP").toEventAdminString(),
         Topics.game(mid, "UpdateMP").toEventAdminString()] as String[]
    }

    @Override
    void setup() {
        trainingSettings = settingsProvider("training", Object)
        trainingSettings?.subscribe { emitStateUpdate() }
    }

    @Override
    Map<String, Object> getInitialState() {
        def settings = trainingSettings?.get()
        if (settings == null) return [settings: [:], isDirty: false, currentHP: 0, maxHP: 100, currentMP: 0, maxMP: 100]
        def trainer = machineContext.getGameModel()?.getTrainer()

        def currentHP = 0, maxHP = 100, currentMP = 0, maxMP = 100
        if (trainer != null) {
            currentHP = trainer.getCurrentHP()
            maxHP = trainer.getMaxHP() ?: 100
            currentMP = trainer.getCurrentMP()
            maxMP = trainer.getMaxMP() ?: 100
        }

        return [
            settings: settings,
            isDirty: trainingSettings.isDirty(),
            currentHP: currentHP,
            maxHP: maxHP,
            currentMP: currentMP,
            maxMP: maxMP
        ]
    }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        try {
            switch (action) {
                case "refresh": break
                case "save": trainingSettings?.save(); break
                case "update":
                    applyFrom(trainingSettings, data, ["hpPotionThreshold", "mpPotionThreshold", "useHpPotion", "useMpPotion"])
                    break
            }
            return withSuccess(getInitialState())
        } catch (Exception e) {
            log.error("Error handling action ${action}", e)
            return withError(e.message)
        }
    }

    @Override
    void handleEvent(Event event) {
        if (event.getTopic().contains("UpdateHP") || event.getTopic().contains("UpdateMP")) {
            emitStateUpdate()
        }
    }
}

new HealingPage()
