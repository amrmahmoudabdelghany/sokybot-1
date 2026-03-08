import org.sokybot.packetsniffer.api.IPacketSnifferPage
import org.sokybot.packetsniffer.api.IPacketSnifferRegistry

class PacketSnifferPage implements IScriptedPage {

    private IMachineContext machineContext
    private IPacketSnifferRegistry registry
    private IPacketSnifferPage sniffer

    @Override
    void init(IMachineContext context) {
        this.machineContext = context
        this.registry = context.getService(IPacketSnifferRegistry)
        this.sniffer = registry?.getSniffer(context.fullName())
    }

    @Override String getTitle() { "Packet Sniffer" }
    @Override String getIcon() { "Activity" }
    @Override Map<String, Object> getSchema() { [:] }
    @Override Map<String, Object> getInitialState() { sniffer != null ? sniffer.getState() : [:] }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        if (sniffer == null) return [success: false, error: "Packet sniffer not available for this machine"]
        return sniffer.handleAction(action, data ?: [:])
    }

    @Override
    Flux<Map<String, Object>> streamData(String streamId, Map<String, Object> params) {
        if (sniffer == null) return Flux.empty()
        if (streamId == "packets" || params?.get("stream") == "packets") return sniffer.streamPackets(params ?: [:])
        if (streamId == "statistics" || params?.get("stream") == "statistics") return sniffer.streamStatistics(params ?: [:])
        return Flux.empty()
    }
}

return new PacketSnifferPage()
