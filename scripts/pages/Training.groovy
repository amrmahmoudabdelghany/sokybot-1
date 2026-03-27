import org.sokybot.machinepages.api.BasePage
import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler

class TrainingPage extends BasePage implements EventHandler {

    def trainingSettings

    TrainingPage() { super("Training", "Target") }

    @Override
    String[] getEventTopics(String machineFullName) {
        def mid = osgiEventTopicSegment(machineFullName)
        ["sokybot/game/${mid}/TrainerStuckEvent"] as String[]
    }

    @Override
    void setup() {
        trainingSettings = settingsProvider("training", Object)
        trainingSettings?.subscribe { emitStateUpdate() }
    }

    @Override
    Map<String, Object> getInitialState() {
        def settings = trainingSettings?.get()
        if (settings == null) return [settings: [:], isDirty: false, activeArea: null]

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
            isDirty: trainingSettings.isDirty(),
            activeArea: activeArea
        ]
    }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        try {
            switch (action) {
                case "refresh": break
                case "save": trainingSettings?.save(); break
                case "update":
                    applyFrom(trainingSettings, data, ["autoAttack", "doNotAttack", "areaX", "areaY", "areaRadius", "activeAreaName"])
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
        if (event.getTopic().contains("TrainerStuckEvent")) {
            emitStateUpdate()
        }
    }
}

new TrainingPage()
