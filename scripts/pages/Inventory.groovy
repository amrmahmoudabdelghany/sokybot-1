import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler
import org.sokybot.machinepages.api.IScriptedPage
import org.sokybot.runtime.IMachineContext
import org.sokybot.gamemodel.model.IItem
import org.sokybot.gamemodel.model.ITrainer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

class InventoryPage implements IScriptedPage, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryPage.class)

    private IMachineContext machineContext
    private final Sinks.Many<Map<String, Object>> stateSink = Sinks.many().multicast().onBackpressureBuffer(100)

    @Override
    String getTitle() { "Inventory" }

    @Override
    String getIcon() { "Box" }

    @Override
    void initialize(IMachineContext context) {
        this.machineContext = context
        log.info("Groovy InventoryPage initialized for {}", context.fullName())
    }

    @Override
    Map<String, Object> getSchema() {
        return [:] // Loaded from Inventory.json
    }

    @Override
    Map<String, Object> getInitialState() {
        return getState()
    }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        log.debug("Handling action: {} with data: {}", action, data)
        switch (action) {
            case "refresh":
                emitStateUpdate()
                break
        }
        def newState = getState()
        newState.put("success", true)
        return newState
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
        // Event logic from InventoryService.java
        // Since we are monitoring ALL inventory events for this machine
        // we can just refresh everything
        log.debug("Inventory event received in Groovy: {}", event.getTopic())
        emitStateUpdate()
    }

    @Override
    void shutdown() {
        stateSink.tryEmitComplete()
    }

    private Map<String, Object> getState() {
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

    private void emitStateUpdate() {
        stateSink.tryEmitNext(getState())
    }
}

new InventoryPage()
