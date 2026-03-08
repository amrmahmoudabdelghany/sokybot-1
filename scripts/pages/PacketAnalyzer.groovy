import org.sokybot.packetsniffer.api.IPacketSnifferPage
import org.sokybot.packetsniffer.api.IPacketSnifferRegistry

class PacketAnalyzerPage implements IScriptedPage {

    private IMachineContext machineContext
    private IPacketSnifferRegistry registry
    private IPacketSnifferPage sniffer

    @Override
    void init(IMachineContext context) {
        this.machineContext = context
        this.registry = context.getService(IPacketSnifferRegistry)
        this.sniffer = registry?.getSniffer(context.fullName())
    }

    @Override String getTitle() { "Packet Analyzer" }
    @Override String getIcon() { "Search" }
    @Override Map<String, Object> getSchema() { [:] }
    @Override Map<String, Object> getInitialState() { sniffer != null ? sniffer.getAnalyzerState() : [packets: [], variables: []] }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        if (sniffer == null) return [success: false, error: "Packet sniffer not available for this machine"]
        if (action == "closeAnalyzer") return sniffer.handleAction("closeAnalyzer", data ?: [:])
        return sniffer.handleAction("analyzerAction", [action: action, data: data ?: [:]])
    }

    @Override
    Flux<Map<String, Object>> streamData(String streamId, Map<String, Object> params) {
        return Flux.empty()
    }
}

return new PacketAnalyzerPage()
