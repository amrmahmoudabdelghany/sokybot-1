import org.sokybot.machinepages.api.BasePage
import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler

class InventoryPage extends BasePage implements EventHandler {

    InventoryPage() { super("Inventory", "Box") }

    @Override
    String[] getEventTopics(String machineFullName) {
        def mid = osgiEventTopicSegment(machineFullName)
        ["sokybot/game/${mid}/InventoryUpdate"] as String[]
    }

    @Override
    Map<String, Object> getInitialState() {
        def items = []
        def trainer = machineContext.getGameModel().getTrainer()
        if (trainer != null && trainer.getInventory() != null) {
            trainer.getInventory().each { item ->
                if (item != null) {
                    items << [
                        slot: item.getSlot(),
                        name: item.getName(),
                        count: item.getStackCount(),
                        refId: String.valueOf(item.getLongId())
                    ]
                }
            }
            items.sort { a, b -> a.slot <=> b.slot }
        }
        return [items: items]
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

new InventoryPage()
