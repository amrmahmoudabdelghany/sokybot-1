import org.sokybot.machinepages.api.BasePage
import org.sokybot.commons.topic.Topics
import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler

class NavigationPage extends BasePage implements EventHandler {

    def trainingSettings

    NavigationPage() { super("Navigation", "Navigation") }

    @Override
    String[] getEventTopics(String machineFullName) {
        def mid = osgiEventTopicSegment(machineFullName)
        [Topics.game(mid, "PositionUpdate").toEventAdminString(),
         Topics.game(mid, "MapChanged").toEventAdminString()] as String[]
    }

    @Override
    void setup() {
        trainingSettings = settingsProvider("training", Object)
        trainingSettings?.subscribe { emitStateUpdate() }
    }

    @Override
    Map<String, Object> getInitialState() {
        def settings = trainingSettings?.get()
        def trainer = machineContext.getGameModel()?.getTrainer()

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

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        try {
            switch (action) {
                case "refresh": break
                case "save": trainingSettings?.save(); break
                case "update":
                    applyFrom(trainingSettings, data, ["loopInTown", "scriptPath"])
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
        if (event.getTopic().contains("PositionUpdate") || event.getTopic().contains("MapChanged")) {
            emitStateUpdate()
        }
    }
}

new NavigationPage()
