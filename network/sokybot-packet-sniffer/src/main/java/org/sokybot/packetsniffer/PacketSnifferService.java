package org.sokybot.packetsniffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.packetsniffer.packettracer.PacketTracerModel;
import org.sokybot.packetsniffer.storage.JsonPacketStorage;
import org.sokybot.packetsniffer.trafficmonitor.TablePacket;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Packet Sniffer Service - Business logic for packet monitoring and tracing
 * Refactored to work with declarative UI system
 */
public class PacketSnifferService {

    private final JsonPacketStorage packetStorage;
    private final String machineName;
    private final List<TablePacket> monitorPackets = new ArrayList<>();
    private final List<PacketTracerModel> tracers = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ObjectMapper mapper = new ObjectMapper();

    // Performance optimizations
    private final BlockingQueue<TablePacket> packetQueue = new ArrayBlockingQueue<>(10000);
    private final ScheduledExecutorService updateExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean updateScheduled = new AtomicBoolean(false);
    private final int BATCH_SIZE = 50;
    private final long UPDATE_INTERVAL_MS = 100;

    // Stream sink for real-time packet updates
    private final Sinks.Many<Map<String, Object>> packetStreamSink = Sinks.many().multicast()
            .onBackpressureBuffer(1000);

    // UI State
    private String activeTab = "traffic";
    private boolean monitorEnabled = true;
    private String tracerSearchFilter = "";
    private PacketTracerModel selectedTracer = null;
    private int packetCount = 0;
    private int lastSentPacketCount = 0;
    private long lastUpdateTime = 0;
    private final Map<String, Object> cachedState = new HashMap<>();

    // State change listeners
    private final List<Consumer<Map<String, Object>>> stateChangeListeners = new CopyOnWriteArrayList<>();

    // Tracer cache for fast lookup
    private final Map<String, PacketTracerModel> tracerCache = new HashMap<>();

    // Analyzer service instance
    private org.sokybot.packetsniffer.packetanalyzer.PacketAnalyzerService analyzerService;

    public org.sokybot.packetsniffer.packetanalyzer.PacketAnalyzerService getAnalyzerService() {
        return analyzerService;
    }

    public PacketSnifferService(JsonPacketStorage storage, String machineName) {
        this.packetStorage = storage;
        this.machineName = machineName;

        // Load saved tracers
        List<PacketTracerModel> savedTracers = storage.load();
        tracers.addAll(savedTracers);

        // Build cache
        for (PacketTracerModel tracer : tracers) {
            String key = tracer.getSource().toString() + ":" + tracer.getOpcode();
            tracerCache.put(key, tracer);
        }

        // Start batched update processor
        startBatchedUpdateProcessor();
    }

    private void startBatchedUpdateProcessor() {
        updateExecutor.scheduleAtFixedRate(() -> {
            if (!packetQueue.isEmpty() && updateScheduled.compareAndSet(false, true)) {
                processBatchedPackets();
            }
        }, UPDATE_INTERVAL_MS, UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void processBatchedPackets() {
        try {
            List<TablePacket> batch = new ArrayList<>();
            packetQueue.drainTo(batch, BATCH_SIZE);

            if (!batch.isEmpty()) {
                try {
                    lock.lock();
                    monitorPackets.addAll(batch);
                    packetCount = monitorPackets.size();

                    // Only send delta if significant change
                    if (packetCount - lastSentPacketCount > 10 ||
                            System.currentTimeMillis() - lastUpdateTime > 500) {
                        notifyStateChangeDelta();
                        lastSentPacketCount = packetCount;
                        lastUpdateTime = System.currentTimeMillis();
                    }
                } finally {
                    lock.unlock();
                }
            }
        } finally {
            updateScheduled.set(false);
        }
    }

    public void display(ImmutablePacket packet) {
        if (!monitorEnabled) {
            return;
        }

        try {
            lock.lock();
            int opcode = packet.getOpcode();
            NetworkPeer source = packet.getPacketSource();

            PacketTracerModel tracer = findTracerFast(source, opcode);

            if (tracer == null) {
                tracer = new PacketTracerModel(source, opcode);
                tracer.setIgnored(false);
                tracer.setName("UNKNOWN");
                tracer.setCount(1);
                tracers.add(tracer);
                String key = source.toString() + ":" + opcode;
                tracerCache.put(key, tracer);
                // Defer save to avoid blocking
                updateExecutor.execute(() -> saveTracers());
            } else {
                tracer.setCount(tracer.getCount() + 1);
            }

            if (!tracer.isIgnored()) {
                TablePacket tablePacket = TablePacket.createTablePacket(tracer.getName(), packet);
                // Non-blocking queue offer
                if (!packetQueue.offer(tablePacket)) {
                    // Queue full, drop oldest
                    packetQueue.poll();
                    packetQueue.offer(tablePacket);
                }

                // Emit to stream
                Map<String, Object> packetData = new HashMap<>();
                packetData.put("type", "packet");
                packetData.put("source", packet.getPacketSource().toString());
                packetData.put("opcode", "0x" + Integer.toHexString(packet.getOpcode() & 0xffff));
                packetData.put("size", packet.getPacketSize());
                packetData.put("name", tracer.getName());
                packetData.put("timestamp", System.currentTimeMillis());
                packetStreamSink.tryEmitNext(packetData);
            }
        } finally {
            lock.unlock();
        }
    }

    private PacketTracerModel findTracerFast(NetworkPeer source, int opcode) {
        String key = source.toString() + ":" + opcode;
        return tracerCache.get(key);
    }

    public void addStateChangeListener(Consumer<Map<String, Object>> listener) {
        stateChangeListeners.add(listener);
    }

    private void notifyStateChange() {
        Map<String, Object> state = getCurrentState();
        for (Consumer<Map<String, Object>> listener : stateChangeListeners) {
            listener.accept(state);
        }
    }

    private void notifyStateChangeDelta() {
        Map<String, Object> delta = new HashMap<>();
        Map<String, Object> currentState = getCurrentState();

        // Only send changed fields
        if (!currentState.get("packetCount").equals(cachedState.get("packetCount"))) {
            delta.put("packetCount", currentState.get("packetCount"));

            // Only send new packets, not entire list
            int newPackets = packetCount - lastSentPacketCount;
            if (newPackets > 0 && newPackets < 100) {
                // Send only new packets
                List<TablePacket> newPacketsList = monitorPackets.subList(
                        Math.max(0, monitorPackets.size() - newPackets),
                        monitorPackets.size());
                delta.put("newPackets", convertPacketsToData(newPacketsList));
            } else {
                // Too many, send count only
                delta.put("packetCount", packetCount);
            }
        }

        if (!delta.isEmpty()) {
            for (Consumer<Map<String, Object>> listener : stateChangeListeners) {
                listener.accept(Map.of("delta", delta, "timestamp", System.currentTimeMillis()));
            }
        }

        cachedState.putAll(currentState);
    }

    public Map<String, Object> handleSchemaRequest(String request) {
        // Return current state for initial render or refresh
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime < 50 && !cachedState.isEmpty()) {
            return Map.of("cached", true, "state", cachedState);
        }
        return getCurrentState();
    }

    public Map<String, Object> handleAction(String action, Map<String, Object> data) {
        try {
            lock.lock();
            Map<String, Object> response = new HashMap<>();

            switch (action) {
                case "switchTab":
                    String tab = (String) data.get("tab");
                    if (tab != null && (tab.equals("traffic") || tab.equals("tracer"))) {
                        activeTab = tab;
                        response.put("success", true);
                        response.put("state", Map.of("activeTab", activeTab));
                    }
                    break;

                case "clearMonitor":
                    monitorPackets.clear();
                    packetQueue.clear();
                    packetCount = 0;
                    lastSentPacketCount = 0;
                    response.put("success", true);
                    response.put("state", Map.of("packetCount", 0, "trafficPackets", new ArrayList<>()));
                    break;

                case "togglePause":
                    monitorEnabled = !monitorEnabled;
                    response.put("success", true);
                    response.put("state", Map.of("monitorEnabled", monitorEnabled));
                    break;

                case "openAnalyzer":
                    // Open analyzer with current packets
                    if (monitorPackets.isEmpty()) {
                        response.put("success", false);
                        response.put("error", "No packets to analyze");
                    } else {
                        analyzerService = new org.sokybot.packetsniffer.packetanalyzer.PacketAnalyzerService(
                                new ArrayList<>(monitorPackets));
                        Map<String, Object> analyzerState = analyzerService.getInitialState();
                        response.put("success", true);
                        response.put("state", analyzerState);
                        response.put("openAnalyzer", true); // Signal to open analyzer page
                    }
                    break;

                case "closeAnalyzer":
                    analyzerService = null;
                    response.put("success", true);
                    response.put("state", Map.of("analyzerOpen", false));
                    break;

                case "analyzerAction":
                    // Delegate to analyzer service
                    if (analyzerService == null) {
                        response.put("success", false);
                        response.put("error", "Analyzer not initialized");
                    } else {
                        String analyzerAction = (String) data.get("action");
                        Map<String, Object> analyzerData = (Map<String, Object>) data.getOrDefault("data",
                                new HashMap<>());

                        Map<String, Object> analyzerResult = handleAnalyzerAction(analyzerAction, analyzerData);
                        response.putAll(analyzerResult);
                    }
                    break;

                case "selectHex":
                case "defineVariable":
                case "updateVariable":
                case "selectVariable":
                case "setGroupLen":
                case "getData":
                    // Analyzer-specific actions
                    if (analyzerService == null) {
                        response.put("success", false);
                        response.put("error", "Analyzer not initialized");
                    } else {
                        Map<String, Object> analyzerResult = handleAnalyzerAction(action, data);
                        response.putAll(analyzerResult);
                    }
                    break;

                case "selectPacket":
                    int index = ((Number) data.getOrDefault("index", -1)).intValue();
                    if (index >= 0 && index < monitorPackets.size()) {
                        TablePacket selected = monitorPackets.get(index);
                        response.put("selectedPacket", Map.of(
                                "source", selected.getPacket().getPacketSource().toString(),
                                "opcode", selected.getPacket().getOpcode()));
                        response.put("success", true);
                    }
                    break;

                case "ignore":
                    String sourceStr = (String) data.get("source");
                    int opcode = parseOpcode((String) data.get("opcode"));
                    NetworkPeer source = parseNetworkPeer(sourceStr);
                    PacketTracerModel tracer = findTracerFast(source, opcode);
                    if (tracer != null) {
                        tracer.setIgnored(true);
                        saveTracers();
                        response.put("success", true);
                    }
                    break;

                case "filterTracer":
                    String filter = (String) data.get("value");
                    if (filter != null) {
                        tracerSearchFilter = filter;
                        response.put("success", true);
                        response.put("state", Map.of("filteredTracers", convertTracersToData(getFilteredTracers())));
                    }
                    break;

                case "selectTracer":
                    sourceStr = (String) data.get("source");
                    opcode = parseOpcode((String) data.get("opcode"));
                    source = parseNetworkPeer(sourceStr);
                    selectedTracer = findTracerFast(source, opcode);
                    response.put("success", true);
                    response.put("state", Map.of(
                            "selectedTracerDescription",
                            selectedTracer != null
                                    ? (selectedTracer.getDescription() != null ? selectedTracer.getDescription() : "")
                                    : ""));
                    break;

                case "updateDescription":
                    String description = (String) data.get("value");
                    if (selectedTracer != null) {
                        selectedTracer.setDescription(description);
                        saveTracers();
                        response.put("success", true);
                    }
                    break;

                case "saveDescription":
                    if (selectedTracer != null) {
                        saveTracers();
                        response.put("success", true);
                    }
                    break;

                case "updateTracerName":
                    sourceStr = (String) data.get("source");
                    opcode = parseOpcode((String) data.get("opcode"));
                    String name = (String) data.get("name");
                    source = parseNetworkPeer(sourceStr);
                    tracer = findTracerFast(source, opcode);
                    if (tracer != null) {
                        tracer.setName(name);
                        saveTracers();
                        response.put("success", true);
                    }
                    break;

                case "toggleIgnore":
                    sourceStr = (String) data.get("source");
                    opcode = parseOpcode((String) data.get("opcode"));
                    source = parseNetworkPeer(sourceStr);
                    tracer = findTracerFast(source, opcode);
                    if (tracer != null) {
                        tracer.setIgnored(!tracer.isIgnored());
                        saveTracers();
                        response.put("success", true);
                    }
                    break;
            }

            // Always return updated state
            response.put("state", getCurrentState());

            notifyStateChange();
            return response;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stream handler for real-time packet updates
     */
    public Flux<Map<String, Object>> streamPackets(Map<String, Object> params) {
        String filter = (String) params.getOrDefault("filter", "");
        boolean includeHistory = (Boolean) params.getOrDefault("includeHistory", false);

        Flux<Map<String, Object>> stream = packetStreamSink.asFlux();

        // Apply filtering if needed
        if (!filter.isEmpty()) {
            stream = stream.filter(data -> {
                String name = (String) data.get("name");
                return name != null && name.toLowerCase().contains(filter.toLowerCase());
            });
        }

        // Include recent history if requested
        if (includeHistory) {
            Flux<Map<String, Object>> history = Flux.fromIterable(getRecentPackets(100));
            return history.concatWith(stream);
        }

        return stream;
    }

    /**
     * Stream handler for statistics updates
     */
    public Flux<Map<String, Object>> streamStatistics(Map<String, Object> params) {
        // Emit statistics every second
        return Flux.interval(java.time.Duration.ofSeconds(1))
                .map(tick -> {
                    try {
                        lock.lock();
                        return Map.of(
                                "packetCount", monitorPackets.size(),
                                "tracerCount", tracers.size(),
                                "queueSize", packetQueue.size(),
                                "monitorEnabled", monitorEnabled,
                                "timestamp", System.currentTimeMillis());
                    } finally {
                        lock.unlock();
                    }
                });
    }

    private List<Map<String, Object>> getRecentPackets(int count) {
        try {
            lock.lock();
            int start = Math.max(0, monitorPackets.size() - count);
            return convertPacketsToData(monitorPackets.subList(start, monitorPackets.size()));
        } finally {
            lock.unlock();
        }
    }

    private List<PacketTracerModel> getFilteredTracers() {
        try {
            lock.lock();
            if (tracerSearchFilter == null || tracerSearchFilter.trim().isEmpty()) {
                return new ArrayList<>(tracers);
            }

            String filter = tracerSearchFilter.toLowerCase();
            List<PacketTracerModel> filtered = new ArrayList<>();
            for (PacketTracerModel tracer : tracers) {
                if (tracer.getName().toLowerCase().contains(filter) ||
                        Integer.toHexString(tracer.getOpcode()).contains(filter) ||
                        tracer.getSource().toString().toLowerCase().contains(filter)) {
                    filtered.add(tracer);
                }
            }
            return filtered;
        } finally {
            lock.unlock();
        }
    }

    private Map<String, Object> getCurrentState() {
        try {
            lock.lock();
            Map<String, Object> state = new HashMap<>();
            state.put("activeTab", activeTab);
            state.put("monitorEnabled", monitorEnabled);
            state.put("tracerSearchFilter", tracerSearchFilter);
            state.put("packetCount", packetCount);
            state.put("monitorIcon", monitorEnabled ? "Pause" : "Play");
            state.put("monitorActionText", monitorEnabled ? "Pause" : "Resume");
            state.put("selectedTracer", selectedTracer != null ? Map.of(
                    "source", selectedTracer.getSource().toString(),
                    "opcode", selectedTracer.getOpcode(),
                    "description", selectedTracer.getDescription() != null ? selectedTracer.getDescription() : "")
                    : Map.of());
            state.put("trafficPackets", convertPacketsToData(monitorPackets));
            state.put("packetTracers", convertTracersToData(tracers));
            state.put("filteredTracers", convertTracersToData(getFilteredTracers()));
            state.put("selectedTracerDescription",
                    selectedTracer != null
                            ? (selectedTracer.getDescription() != null ? selectedTracer.getDescription() : "")
                            : "");
            return state;
        } finally {
            lock.unlock();
        }
    }

    private NetworkPeer parseNetworkPeer(String sourceStr) {
        try {
            return NetworkPeer.valueOf(sourceStr);
        } catch (Exception e) {
            return NetworkPeer.CLIENT; // Default fallback
        }
    }

    private int parseOpcode(String opcodeStr) {
        if (opcodeStr == null)
            return 0;
        if (opcodeStr.startsWith("0x")) {
            return Integer.parseInt(opcodeStr.substring(2), 16);
        }
        return Integer.parseInt(opcodeStr, 16);
    }

    private void saveTracers() {
        packetStorage.save(tracers);
    }

    private List<Map<String, Object>> convertPacketsToData(List<TablePacket> packets) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (TablePacket packet : packets) {
            ImmutablePacket p = packet.getPacket();
            Map<String, Object> row = new HashMap<>();
            row.put("source", p.getPacketSource().toString());
            row.put("encoding", p.getPacketEncoding().toString());
            row.put("size", p.getPacketSize());
            row.put("name", packet.getName());
            row.put("opcode", "0x" + Integer.toHexString(p.getOpcode() & 0xffff));
            row.put("count", "0x" + Integer.toHexString(p.getCount() & 0xff));
            row.put("crc", "0x" + Integer.toHexString(p.getCRC() & 0xff));
            result.add(row);
        }
        return result;
    }

    private Map<String, Object> handleAnalyzerAction(String action, Map<String, Object> data) {
        if (analyzerService == null) {
            return Map.of("success", false, "error", "Analyzer not initialized");
        }

        switch (action) {
            case "getData":
                return analyzerService.getInitialState();

            case "selectHex":
                String hex = (String) data.get("hex");
                int startOffset = ((Number) data.getOrDefault("startOffset", -1)).intValue();
                int endOffset = ((Number) data.getOrDefault("endOffset", -1)).intValue();
                return analyzerService.handleSelectHex(hex, startOffset, endOffset);

            case "defineVariable":
                String varName = (String) data.get("varName");
                String hexValue = (String) data.get("hex");
                String packetName = (String) data.get("packetName");
                return analyzerService.defineVariable(varName, hexValue, packetName);

            case "updateVariable":
                int index = ((Number) data.getOrDefault("index", -1)).intValue();
                String newVarName = (String) data.get("varName");
                String comment = (String) data.get("comment");
                return analyzerService.updateVariable(index, newVarName, comment);

            case "selectVariable":
                int varIndex = ((Number) data.getOrDefault("index", -1)).intValue();
                return analyzerService.selectVariable(varIndex);

            case "setGroupLen":
                int groupLen = ((Number) data.getOrDefault("groupLen", 16)).intValue();
                return analyzerService.setGroupLen(groupLen);

            default:
                return Map.of("success", false, "error", "Unknown analyzer action: " + action);
        }
    }

    private List<Map<String, Object>> convertTracersToData(List<PacketTracerModel> tracers) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (PacketTracerModel tracer : tracers) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", tracer.getName());
            row.put("source", tracer.getSource().toString());
            row.put("opcode", "0x" + Integer.toHexString(tracer.getOpcode() & 0xffff));
            row.put("count", tracer.getCount());
            row.put("ignored", tracer.isIgnored());
            row.put("description", tracer.getDescription() != null ? tracer.getDescription() : "");
            result.add(row);
        }
        return result;
    }

    public void shutdown() {
        updateExecutor.shutdown();
        try {
            if (!updateExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                updateExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            updateExecutor.shutdownNow();
        }
    }
}
