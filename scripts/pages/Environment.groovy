import org.sokybot.machinepages.api.BasePage
import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler

class EnvironmentPage extends BasePage implements EventHandler {

    EnvironmentPage() { super("Environment", "Globe") }

    @Override
    String[] getEventTopics(String machineFullName) {
        ["sokybot/game/${machineFullName}/EntitySpawn",
         "sokybot/game/${machineFullName}/EntityDespawn"] as String[]
    }

    @Override
    Map<String, Object> getInitialState() {
        return [
            monsterPreferences: [:],
            areaFilters: [:]
        ]
    }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        if (action == "refresh") emitStateUpdate()
        return withSuccess(getInitialState())
    }

    @Override
    void handleEvent(Event event) {
        emitStateUpdate()
    }
}

new EnvironmentPage()
