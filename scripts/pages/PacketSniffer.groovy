import org.sokybot.machinepages.api.IScriptedPage
import org.sokybot.packetsniffer.api.IPacketSnifferPage
import org.sokybot.packetsniffer.api.IPacketSnifferRegistry
import org.sokybot.runtime.IMachineContext
import reactor.core.publisher.Flux

/**
 * Scripted page for Packet Sniffer. Delegates state, actions, and streams to the
 * packet sniffer service for this machine (from IPacketSnifferRegistry).
 */
class PacketSnifferPage implements IScriptedPage {

    private IMachineContext machineContext
    private IPacketSnifferRegistry registry
    private IPacketSnifferPage sniffer

    @Override
    void init(IMachineContext context) {
        this.machineContext = context
        this.registry = context.getSokybotContext()?.getService(IPacketSnifferRegistry.class)
        this.sniffer = registry?.getSniffer(context.fullName())
    }

    @Override
    String getTitle() {
        return "Packet Sniffer"
    }

    @Override
    String getIcon() {
        return "Activity"
    }

    @Override
    Map<String, Object> getSchema() {
        return [:]  // Loaded from PacketSniffer.json by ScriptPageLoader
    }

    @Override
    Map<String, Object> getInitialState() {
        return sniffer != null ? sniffer.getState() : [:]
    }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        if (sniffer == null) {
            return [success: false, error: "Packet sniffer not available for this machine"]
        }
        return sniffer.handleAction(action, data != null ? data : [:])
    }

    @Override
    Flux<Map<String, Object>> streamData(String streamId, Map<String, Object> params) {
        if (sniffer == null) {
            return Flux.empty()
        }
        if (streamId == "packets" || (params != null && "packets" == params.get("stream"))) {
            return sniffer.streamPackets(params != null ? params : [:])
        }
        if (streamId == "statistics" || (params != null && "statistics" == params.get("stream"))) {
            return sniffer.streamStatistics(params != null ? params : [:])
        }
        return Flux.empty()
    }
}

return new PacketSnifferPage()
