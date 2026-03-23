package org.sokybot.packetsniffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.packetsniffer.api.IPacketSnifferPage;
import org.sokybot.packetsniffer.filter.PacketFilter;
import org.sokybot.packetsniffer.packetanalyzer.PacketAnalyzerService;
import org.sokybot.packetsniffer.packettracer.PacketTracerModel;
import org.sokybot.packetsniffer.storage.JsonPacketStorage;
import org.sokybot.packetsniffer.storage.StructDefinitionStorage;
import org.sokybot.packetsniffer.struct.StructDefinition;
import org.sokybot.packetsniffer.struct.StructField;
import org.sokybot.packetsniffer.trafficmonitor.TablePacket;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.proxy.recording.IPacketRecorder;
import org.sokybot.proxy.recording.PacketDirection;
import org.sokybot.proxy.recording.PacketRecording;
import org.sokybot.proxy.recording.RecordedPacket;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Packet Sniffer Service - Business logic for packet monitoring and tracing.
 * Implements IPacketSnifferPage for use by scripted pages (PacketSniffer,
 * PacketAnalyzer).
 */
public class PacketSnifferService implements IPacketSnifferPage {
    private static final DateTimeFormatter PACKET_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final JsonPacketStorage packetStorage;
    private final String machineName;
    private final List<TablePacket> monitorPackets = new ArrayList<>();
    private final List<PacketTracerModel> tracers = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final IProxyConnection proxyConnection;
    private final IPacketRecorder packetRecorder;
    private final StructDefinitionStorage structStorage;

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
    private String monitorFilterQuery = "";
    private String monitorFilterError = "";
    private PacketFilter.Predicate monitorFilterPredicate = row -> true;
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
    private PacketAnalyzerService analyzerService;

    private final List<Integer> selectedPacketIndices = new ArrayList<>();
    private boolean diffOpen = false;
    private List<Map<String, Object>> diffA = new ArrayList<>();
    private List<Map<String, Object>> diffB = new ArrayList<>();
    private boolean replayOpen = false;
    private String replayDirection = "C2S";
    private List<Map<String, Object>> replayPacketRows = new ArrayList<>();

    private final Map<Integer, StructDefinition> structDefinitions = new HashMap<>();
    private final Map<String, Integer> opcodeFrequency = new HashMap<>();

    private boolean recordingActive = false;
    private PacketRecording lastRecording;

    public PacketAnalyzerService getAnalyzerService() {
        return analyzerService;
    }

    public PacketSnifferService(JsonPacketStorage storage, String machineName) {
        this(storage, machineName, null, null, new StructDefinitionStorage("./struct-definitions.json"));
    }

    public PacketSnifferService(
            JsonPacketStorage storage,
            String machineName,
            IProxyConnection proxyConnection,
            IPacketRecorder packetRecorder,
            StructDefinitionStorage structStorage) {
        this.packetStorage = storage;
        this.machineName = machineName;
        this.proxyConnection = proxyConnection;
        this.packetRecorder = packetRecorder;
        this.structStorage = structStorage;

        // Load saved tracers
        List<PacketTracerModel> savedTracers = storage.load();
        tracers.addAll(savedTracers);
        for (StructDefinition definition : structStorage.load()) {
            structDefinitions.put(definition.getOpcode(), definition);
        }

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
            String frequencyKey = source + ":" + String.format("0x%04X", opcode & 0xFFFF);
            opcodeFrequency.merge(frequencyKey, 1, Integer::sum);

            if (tracer == null) {
                tracer = new PacketTracerModel(source, opcode);
                tracer.setIgnored(false);
                tracer.setName(resolveOpcodeName(source, opcode));
                tracer.setCount(1);
                tracers.add(tracer);
                String key = source.toString() + ":" + opcode;
                tracerCache.put(key, tracer);
                updateExecutor.execute(() -> saveTracers());
            } else {
                tracer.setCount(tracer.getCount() + 1);
            }

            if (!tracer.isIgnored()) {
                TablePacket tablePacket = TablePacket.createTablePacket(tracer.getName(), packet,
                        monitorPackets.size());
                Map<String, Object> row = buildPacketRow(packet, tracer.getName(), tablePacket.getIndex());
                if (!matchesCurrentFilter(row)) {
                    return;
                }
                if (!packetQueue.offer(tablePacket)) {
                    packetQueue.poll();
                    packetQueue.offer(tablePacket);
                }
                packetStreamSink.tryEmitNext(row);
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

    @Override
    public Map<String, Object> getState() {
        return handleSchemaRequest("");
    }

    @Override
    public Map<String, Object> getAnalyzerState() {
        if (analyzerService == null) {
            return Map.of("packets", new ArrayList<>(), "variables", new ArrayList<>());
        }
        return analyzerService.getInitialState();
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
                    opcodeFrequency.clear();
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
                    // Open analyzer with current packets (state merged in getCurrentState() below)
                    if (getFilteredMonitorPackets().isEmpty()) {
                        response.put("success", false);
                        response.put("error", "No packets to analyze");
                    } else {
                        analyzerService = new PacketAnalyzerService(new ArrayList<>(getFilteredMonitorPackets()));
                        response.put("success", true);
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

                case "focusTracer":
                    sourceStr = (String) data.get("source");
                    opcode = parseOpcode((String) data.get("opcode"));
                    source = parseNetworkPeer(sourceStr);
                    monitorFilterQuery = String.format("opcode:0x%04X AND source:%s", opcode & 0xFFFF, source);
                    try {
                        monitorFilterPredicate = PacketFilter.parse(monitorFilterQuery);
                        monitorFilterError = "";
                    } catch (Exception e) {
                        monitorFilterError = e.getMessage();
                    }
                    activeTab = "traffic";
                    response.put("success", true);
                    response.put("state", Map.of(
                            "activeTab", activeTab,
                            "monitorFilterQuery", monitorFilterQuery,
                            "monitorFilterError", monitorFilterError,
                            "trafficPackets", convertPacketsToData(getFilteredMonitorPackets())));
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

                case "filterMonitor":
                    String query = (String) data.getOrDefault("value", "");
                    monitorFilterQuery = query != null ? query.trim() : "";
                    try {
                        monitorFilterPredicate = PacketFilter.parse(monitorFilterQuery);
                        monitorFilterError = "";
                        response.put("success", true);
                    } catch (Exception e) {
                        monitorFilterError = e.getMessage();
                        response.put("success", false);
                        response.put("error", monitorFilterError);
                        // Keep the previous valid predicate so we do not break monitoring.
                    }
                    response.put("state", Map.of(
                            "monitorFilterQuery", monitorFilterQuery,
                            "monitorFilterError", monitorFilterError,
                            "trafficPackets", convertPacketsToData(getFilteredMonitorPackets())));
                    break;

                case "selectPackets":
                    selectedPacketIndices.clear();
                    List<?> list = (List<?>) data.getOrDefault("indices", new ArrayList<>());
                    for (Object value : list) {
                        if (value instanceof Number) {
                            int i = ((Number) value).intValue();
                            if (i >= 0 && i < monitorPackets.size()) {
                                selectedPacketIndices.add(i);
                            }
                        }
                    }
                    response.put("success", true);
                    response.put("state", Map.of("selectedPacketIndices", new ArrayList<>(selectedPacketIndices)));
                    break;

                case "diffPackets":
                    if (selectedPacketIndices.size() != 2) {
                        response.put("success", false);
                        response.put("error", "Select exactly two packets for diff");
                        break;
                    }
                    TablePacket packetA = monitorPackets.get(selectedPacketIndices.get(0));
                    TablePacket packetB = monitorPackets.get(selectedPacketIndices.get(1));
                    if (packetA.getPacket().getOpcode() != packetB.getPacket().getOpcode()) {
                        response.put("success", false);
                        response.put("error", "Selected packets must have the same opcode");
                        break;
                    }
                    byte[] bytesA = packetA.getPacket().toBytes();
                    byte[] bytesB = packetB.getPacket().toBytes();
                    List<Integer> changedOffsets = diffOffsets(bytesA, bytesB);
                    diffA = toHexRows(packetA, changedOffsets, groupLenFromData(data));
                    diffB = toHexRows(packetB, changedOffsets, groupLenFromData(data));
                    diffOpen = true;
                    response.put("success", true);
                    break;

                case "closeDiff":
                    diffOpen = false;
                    diffA = new ArrayList<>();
                    diffB = new ArrayList<>();
                    response.put("success", true);
                    break;

                case "openReplay":
                    int replayIndex = ((Number) data.getOrDefault("index", -1)).intValue();
                    if (replayIndex < 0 || replayIndex >= monitorPackets.size()) {
                        response.put("success", false);
                        response.put("error", "Invalid packet index");
                        break;
                    }
                    TablePacket replayPacket = monitorPackets.get(replayIndex);
                    replayPacketRows = toHexRows(replayPacket, List.of(), 16);
                    replayDirection = replayPacket.getPacket().getPacketSource() == NetworkPeer.SERVER ? "S2C" : "C2S";
                    replayOpen = true;
                    response.put("success", true);
                    break;

                case "closeReplay":
                    replayOpen = false;
                    replayPacketRows = new ArrayList<>();
                    response.put("success", true);
                    break;

                case "defineField":
                    response.putAll(handleDefineField(data));
                    break;
                case "updateField":
                    response.putAll(handleUpdateField(data));
                    break;
                case "removeField":
                    response.putAll(handleRemoveField(data));
                    break;
                case "listStructDefinitions":
                    response.put("success", true);
                    response.put("structDefinitions", convertStructDefinitions());
                    break;

                case "toggleRecording":
                    response.putAll(handleToggleRecording());
                    break;
                case "exportSelected":
                    response.putAll(handleExportSelected());
                    break;
                case "exportRecording":
                    response.putAll(handleExportRecording());
                    break;

                case "injectPacket":
                    response.putAll(handleInjectPacket(data));
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
        stream = stream.filter(this::matchesCurrentFilter);

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
            List<TablePacket> filtered = getFilteredMonitorPackets();
            int start = Math.max(0, filtered.size() - count);
            return convertPacketsToData(filtered.subList(start, filtered.size()));
        } finally {
            lock.unlock();
        }
    }

    private List<TablePacket> getFilteredMonitorPackets() {
        if (monitorFilterQuery.isBlank()) {
            return new ArrayList<>(monitorPackets);
        }
        return monitorPackets.stream()
                .filter(packet -> matchesCurrentFilter(
                        buildPacketRow(packet.getPacket(), packet.getName(), packet.getIndex())))
                .collect(Collectors.toList());
    }

    private boolean matchesCurrentFilter(Map<String, Object> row) {
        try {
            return monitorFilterPredicate == null || monitorFilterPredicate.test(row);
        } catch (Exception e) {
            monitorFilterError = "Filter runtime error: " + e.getMessage();
            return true;
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
            int serverCount = 0;
            int clientCount = 0;
            int botCount = 0;
            for (TablePacket packet : monitorPackets) {
                if (packet == null || packet.getPacket() == null || packet.getPacket().getPacketSource() == null) {
                    continue;
                }
                switch (packet.getPacket().getPacketSource()) {
                    case SERVER:
                        serverCount++;
                        break;
                    case CLIENT:
                        clientCount++;
                        break;
                    case BOT:
                        botCount++;
                        break;
                    default:
                        break;
                }
            }
            state.put("activeTab", activeTab);
            state.put("monitorEnabled", monitorEnabled);
            state.put("tracerSearchFilter", tracerSearchFilter);
            state.put("monitorFilterQuery", monitorFilterQuery);
            state.put("monitorFilterError", monitorFilterError);
            state.put("monitorFilterHint", PacketFilter.supportedSyntaxHint());
            state.put("packetCount", packetCount);
            state.put("serverCount", serverCount);
            state.put("clientCount", clientCount);
            state.put("botCount", botCount);
            state.put("topOpcodes", getTopOpcodes());
            state.put("monitorIcon", monitorEnabled ? "Pause" : "Play");
            state.put("monitorActionText", monitorEnabled ? "Pause" : "Resume");
            state.put("selectedTracer", selectedTracer != null ? Map.of(
                    "source", selectedTracer.getSource().toString(),
                    "opcode", selectedTracer.getOpcode(),
                    "description", selectedTracer.getDescription() != null ? selectedTracer.getDescription() : "")
                    : Map.of());
            state.put("trafficPackets", convertPacketsToData(getFilteredMonitorPackets()));
            state.put("packetTracers", convertTracersToData(tracers));
            state.put("filteredTracers", convertTracersToData(getFilteredTracers()));
            state.put("selectedPacketIndices", new ArrayList<>(selectedPacketIndices));
            state.put("diffOpen", diffOpen);
            state.put("diffPacketsA", diffA);
            state.put("diffPacketsB", diffB);
            state.put("recordingActive", recordingActive);
            state.put("structDefinitions", convertStructDefinitions());
            state.put("replayOpen", replayOpen);
            state.put("replayDirection", replayDirection);
            state.put("replayPacketRows", replayPacketRows);
            state.put("selectedTracerDescription",
                    selectedTracer != null
                            ? (selectedTracer.getDescription() != null ? selectedTracer.getDescription() : "")
                            : "");
            state.put("analyzerOpen", analyzerService != null);
            if (analyzerService != null) {
                state.putAll(analyzerService.getInitialState());
            }
            state.put("decodedFields", getLatestDecodedFields());
            return state;
        } finally {
            lock.unlock();
        }
    }

    private List<Map<String, Object>> getLatestDecodedFields() {
        List<TablePacket> filtered = getFilteredMonitorPackets();
        if (filtered.isEmpty()) {
            return List.of();
        }
        TablePacket last = filtered.get(filtered.size() - 1);
        return decodeStructFields(last.getPacket());
    }

    private List<Map<String, Object>> getTopOpcodes() {
        return opcodeFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(entry -> {
                    String[] parts = entry.getKey().split(":", 2);
                    String source = parts.length > 0 ? parts[0] : "UNKNOWN";
                    String opcode = parts.length > 1 ? parts[1] : "0x0000";
                    int opcodeValue = parseOpcode(opcode);
                    return Map.of(
                            "source", source,
                            "opcode", opcode,
                            "name", resolveOpcodeName(parseNetworkPeer(source), opcodeValue),
                            "count", entry.getValue());
                })
                .collect(Collectors.toList());
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
        if (opcodeStr.matches("^[0-9]+$")) {
            return Integer.parseInt(opcodeStr, 10);
        }
        return Integer.parseInt(opcodeStr, 16);
    }

    private void saveTracers() {
        packetStorage.save(tracers);
    }

    private List<Map<String, Object>> convertPacketsToData(List<TablePacket> packets) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (TablePacket packet : packets) {
            result.add(buildPacketRow(packet.getPacket(), packet.getName(), packet.getIndex()));
        }
        return result;
    }

    private Map<String, Object> buildPacketRow(ImmutablePacket p, String name, int index) {
        Map<String, Object> row = new HashMap<>();
        long now = System.currentTimeMillis();
        row.put("index", index);
        row.put("source", p.getPacketSource().toString());
        row.put("encoding", p.getPacketEncoding().toString());
        row.put("size", p.getPacketSize());
        row.put("name", name);
        row.put("opcode", "0x" + Integer.toHexString(p.getOpcode() & 0xffff));
        row.put("count", "0x" + Integer.toHexString(p.getCount() & 0xff));
        row.put("crc", "0x" + Integer.toHexString(p.getCRC() & 0xff));
        row.put("payload", bytesToHex(p.toBytes()));
        row.put("opcodeValue", p.getOpcode());
        row.put("decodedFields", decodeStructFields(p));
        row.put("timestamp", now);
        row.put("time",
                LocalTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault()).format(PACKET_TIME_FORMATTER));
        return row;
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private static final Map<Integer, String> KNOWN_OPCODES = Map.ofEntries(
            Map.entry(0x5000, "SETUP"),
            Map.entry(0x5001, "CHALLENGE"),
            Map.entry(0x2001, "MODULE_ID"),
            Map.entry(0x9000, "HANDSHAKE_ACCEPT"),
            Map.entry(0x2002, "PATCH_INFO"),
            Map.entry(0xA100, "AUTH_REQUEST"),
            Map.entry(0xA101, "AUTH_RESPONSE"),
            Map.entry(0xA102, "LOGIN_RESPONSE"),
            Map.entry(0xA103, "SERVER_LIST"),
            Map.entry(0x6005, "AGENT_REQUEST"),
            Map.entry(0x600D, "MASSIVE"),
            Map.entry(0x34B5, "TELEPORT_COMPLETE"),
            Map.entry(0x3020, "CHARACTER_DATA"),
            Map.entry(0x3013, "ENTITY_SPAWN"),
            Map.entry(0x3015, "ENTITY_DESPAWN"));

    private static String resolveOpcodeName(NetworkPeer source, int opcode) {
        String known = KNOWN_OPCODES.get(opcode);
        return known != null ? known : "UNKNOWN";
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

    private int groupLenFromData(Map<String, Object> data) {
        Object value = data.get("groupLen");
        if (value instanceof Number) {
            int g = ((Number) value).intValue();
            return Math.max(8, Math.min(32, g));
        }
        return 16;
    }

    private List<Integer> diffOffsets(byte[] a, byte[] b) {
        List<Integer> changed = new ArrayList<>();
        int max = Math.max(a.length, b.length);
        for (int i = 0; i < max; i++) {
            byte av = i < a.length ? a[i] : 0;
            byte bv = i < b.length ? b[i] : 0;
            if (i >= a.length || i >= b.length || av != bv) {
                changed.add(i);
            }
        }
        return changed;
    }

    private List<Map<String, Object>> toHexRows(TablePacket tablePacket, List<Integer> changedOffsets, int groupLen) {
        List<Map<String, Object>> rows = new ArrayList<>();
        byte[] buffer = tablePacket.getPacket().toBytes();
        Map<String, Object> headerRow = new HashMap<>();
        headerRow.put("type", "header");
        headerRow.put("source", tablePacket.getPacket().getPacketSource().toString());
        headerRow.put("opcode", String.format("0x%04X", tablePacket.getPacket().getOpcode() & 0xffff));
        headerRow.put("name", tablePacket.getName());
        rows.add(headerRow);

        int i = 0;
        while (i < buffer.length) {
            int start = i;
            int end = Math.min(i + groupLen, buffer.length);
            Map<String, Object> dataRow = new HashMap<>();
            dataRow.put("type", "data");
            dataRow.put("lineNumber", String.format("%06X", start));
            dataRow.put("hex", toHexLine(buffer, start, end));
            dataRow.put("ascii", toAsciiLine(buffer, start, end));
            dataRow.put("startOffset", start);
            dataRow.put("endOffset", end);
            dataRow.put("changedOffsets",
                    changedOffsets.stream().filter(v -> v >= start && v < end).collect(Collectors.toList()));
            rows.add(dataRow);
            i += groupLen;
        }
        rows.add(Map.of("type", "separator"));
        return rows;
    }

    private String toHexLine(byte[] bytes, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }

    private String toAsciiLine(byte[] bytes, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            int c = bytes[i] & 0xFF;
            sb.append(c >= 32 && c <= 126 ? (char) c : '.');
        }
        return sb.toString();
    }

    private Map<String, Object> handleDefineField(Map<String, Object> data) {
        int opcode = parseOpcode(String.valueOf(data.getOrDefault("opcode", "0")));
        String name = String.valueOf(data.getOrDefault("name", "field_" + opcode));
        String type = String.valueOf(data.getOrDefault("type", "bytes"));
        int offset = ((Number) data.getOrDefault("offset", 0)).intValue();
        int length = ((Number) data.getOrDefault("length", 1)).intValue();
        String endian = String.valueOf(data.getOrDefault("endian", "little"));

        StructDefinition definition = structDefinitions.computeIfAbsent(opcode, k -> {
            StructDefinition def = new StructDefinition();
            def.setOpcode(opcode);
            return def;
        });

        StructField field = new StructField();
        field.setName(name);
        field.setType(type);
        field.setOffset(offset);
        field.setLength(Math.max(1, length));
        field.setEndian(endian);
        field.setComment(String.valueOf(data.getOrDefault("comment", "")));
        definition.getFields().add(field);
        persistStructDefinitions();
        return Map.of("success", true, "structDefinitions", convertStructDefinitions());
    }

    private Map<String, Object> handleUpdateField(Map<String, Object> data) {
        int opcode = parseOpcode(String.valueOf(data.getOrDefault("opcode", "0")));
        int index = ((Number) data.getOrDefault("index", -1)).intValue();
        StructDefinition definition = structDefinitions.get(opcode);
        if (definition == null || index < 0 || index >= definition.getFields().size()) {
            return Map.of("success", false, "error", "Field not found");
        }
        StructField field = definition.getFields().get(index);
        if (data.containsKey("name")) {
            field.setName(String.valueOf(data.get("name")));
        }
        if (data.containsKey("type")) {
            field.setType(String.valueOf(data.get("type")));
        }
        if (data.containsKey("offset")) {
            field.setOffset(((Number) data.get("offset")).intValue());
        }
        if (data.containsKey("length")) {
            field.setLength(Math.max(1, ((Number) data.get("length")).intValue()));
        }
        if (data.containsKey("comment")) {
            field.setComment(String.valueOf(data.get("comment")));
        }
        if (data.containsKey("endian")) {
            field.setEndian(String.valueOf(data.get("endian")));
        }
        persistStructDefinitions();
        return Map.of("success", true);
    }

    private Map<String, Object> handleRemoveField(Map<String, Object> data) {
        int opcode = parseOpcode(String.valueOf(data.getOrDefault("opcode", "0")));
        int index = ((Number) data.getOrDefault("index", -1)).intValue();
        StructDefinition definition = structDefinitions.get(opcode);
        if (definition == null || index < 0 || index >= definition.getFields().size()) {
            return Map.of("success", false, "error", "Field not found");
        }
        definition.getFields().remove(index);
        persistStructDefinitions();
        return Map.of("success", true);
    }

    private void persistStructDefinitions() {
        structStorage.save(new ArrayList<>(structDefinitions.values()));
    }

    private List<Map<String, Object>> convertStructDefinitions() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (StructDefinition def : structDefinitions.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("opcode", String.format("0x%04X", def.getOpcode() & 0xffff));
            row.put("opcodeValue", def.getOpcode());
            List<Map<String, Object>> fields = new ArrayList<>();
            for (int i = 0; i < def.getFields().size(); i++) {
                StructField field = def.getFields().get(i);
                fields.add(Map.of(
                        "index", i,
                        "name", field.getName(),
                        "type", field.getType(),
                        "offset", field.getOffset(),
                        "length", field.getLength(),
                        "comment", field.getComment() == null ? "" : field.getComment(),
                        "endian", field.getEndian() == null ? "little" : field.getEndian()));
            }
            row.put("fields", fields);
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> decodeStructFields(ImmutablePacket packet) {
        StructDefinition definition = structDefinitions.get(packet.getOpcode());
        if (definition == null || definition.getFields().isEmpty()) {
            return List.of();
        }
        byte[] payload = packet.toBytes();
        List<Map<String, Object>> decoded = new ArrayList<>();
        for (StructField field : definition.getFields()) {
            int start = Math.max(0, field.getOffset());
            int end = Math.min(payload.length, start + Math.max(1, field.getLength()));
            if (start >= end) {
                continue;
            }
            byte[] raw = Arrays.copyOfRange(payload, start, end);
            decoded.add(Map.of(
                    "name", field.getName(),
                    "type", field.getType(),
                    "offset", field.getOffset(),
                    "length", field.getLength(),
                    "hexValue", bytesToHex(raw),
                    "decodedValue", decodeFieldValue(raw, field)));
        }
        return decoded;
    }

    private String decodeFieldValue(byte[] raw, StructField field) {
        String type = field.getType() == null ? "bytes" : field.getType().toLowerCase(Locale.ROOT);
        ByteOrder order = "big".equalsIgnoreCase(field.getEndian()) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        ByteBuffer buffer = ByteBuffer.wrap(Arrays.copyOf(raw, Math.max(raw.length, 8))).order(order);
        switch (type) {
            case "uint8":
                return String.valueOf(raw[0] & 0xFF);
            case "uint16":
                return String.valueOf(buffer.getShort(0) & 0xFFFF);
            case "uint32":
                return String.valueOf(buffer.getInt(0) & 0xFFFFFFFFL);
            case "int16":
                return String.valueOf(buffer.getShort(0));
            case "int32":
                return String.valueOf(buffer.getInt(0));
            case "float":
                return String.valueOf(buffer.getFloat(0));
            case "string":
                return new String(raw, StandardCharsets.UTF_8).replace("\0", "");
            default:
                return bytesToHex(raw);
        }
    }

    private Map<String, Object> handleToggleRecording() {
        if (packetRecorder == null) {
            return Map.of("success", false, "error", "Packet recorder unavailable");
        }
        if (!recordingActive) {
            boolean started = packetRecorder.startRecording(machineName);
            if (!started) {
                return Map.of("success", false, "error", "Recording already active");
            }
            recordingActive = true;
            return Map.of("success", true, "recordingActive", true);
        }
        var stopped = packetRecorder.stopRecording(machineName);
        if (stopped.isEmpty()) {
            return Map.of("success", false, "error", "No active recording");
        }
        recordingActive = false;
        lastRecording = stopped.get();
        return Map.of("success", true, "recordingActive", false, "recordedPackets", lastRecording.getPacketCount());
    }

    private Map<String, Object> handleExportRecording() {
        if (lastRecording == null) {
            return Map.of("success", false, "error", "No completed recording to export");
        }
        return buildExportResponse(lastRecording.getPackets(), "recording_" + machineName + ".hex");
    }

    private Map<String, Object> handleExportSelected() {
        if (selectedPacketIndices.isEmpty()) {
            return Map.of("success", false, "error", "No selected packets");
        }
        List<RecordedPacket> packets = new ArrayList<>();
        for (Integer idx : selectedPacketIndices) {
            if (idx >= 0 && idx < monitorPackets.size()) {
                TablePacket p = monitorPackets.get(idx);
                PacketDirection direction = p.getPacket().getPacketSource() == NetworkPeer.SERVER
                        ? PacketDirection.TO_CLIENT
                        : PacketDirection.TO_SERVER;
                packets.add(RecordedPacket.now(direction, p.getPacket().getOpcode(), p.getPacket().toBytes()));
            }
        }
        return buildExportResponse(packets, "selected_packets_" + machineName + ".hex");
    }

    private Map<String, Object> buildExportResponse(List<RecordedPacket> packets, String filename) {
        String hexDump = packetsToHexDump(packets);
        String binBase64 = Base64.getEncoder().encodeToString(packetsToBinary(packets));
        return Map.of(
                "success", true,
                "filename", filename,
                "hexDump", hexDump,
                "binaryBase64", binBase64,
                "packetCount", packets.size());
    }

    private String packetsToHexDump(List<RecordedPacket> packets) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        StringBuilder out = new StringBuilder();
        for (RecordedPacket packet : packets) {
            out.append("# ").append(formatter.format(packet.getTimestamp()))
                    .append(" ").append(packet.getDirection())
                    .append(" opcode=0x").append(String.format("%04X", packet.getOpcode() & 0xFFFF))
                    .append(" len=").append(packet.getDataLength()).append('\n');
            byte[] data = packet.getData();
            for (int i = 0; i < data.length; i += 16) {
                int end = Math.min(i + 16, data.length);
                out.append(String.format("%06X  ", i));
                for (int j = i; j < end; j++) {
                    out.append(String.format("%02X ", data[j]));
                }
                out.append(" | ");
                for (int j = i; j < end; j++) {
                    int c = data[j] & 0xFF;
                    out.append(c >= 32 && c <= 126 ? (char) c : '.');
                }
                out.append('\n');
            }
            out.append('\n');
        }
        return out.toString();
    }

    private byte[] packetsToBinary(List<RecordedPacket> packets) {
        int total = 0;
        for (RecordedPacket p : packets) {
            total += 1 + 4 + p.getDataLength();
        }
        ByteBuffer buffer = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        for (RecordedPacket p : packets) {
            buffer.put((byte) (p.getDirection() == PacketDirection.TO_SERVER ? 0 : 1));
            buffer.putInt(p.getDataLength());
            buffer.put(p.getData());
        }
        return buffer.array();
    }

    private Map<String, Object> handleInjectPacket(Map<String, Object> data) {
        if (proxyConnection == null) {
            return Map.of("success", false, "error", "Proxy connection unavailable");
        }
        String hexPayload = String.valueOf(data.getOrDefault("hexPayload", ""));
        String direction = String.valueOf(data.getOrDefault("direction", "C2S"));
        byte[] bytes = parseHexPayload(hexPayload);
        if (bytes.length == 0) {
            return Map.of("success", false, "error", "Empty payload");
        }
        MutablePacket packet = MutablePacket.wrap(bytes);
        if ("S2C".equalsIgnoreCase(direction)) {
            proxyConnection.sendToClient(packet);
        } else {
            proxyConnection.sendToServer(packet);
        }
        return Map.of("success", true, "injected", true);
    }

    private byte[] parseHexPayload(String payload) {
        String normalized = payload == null ? "" : payload.replaceAll("[^A-Fa-f0-9]", "");
        if (normalized.length() < 2) {
            return new byte[0];
        }
        if ((normalized.length() & 1) == 1) {
            normalized = "0" + normalized;
        }
        byte[] out = new byte[normalized.length() / 2];
        for (int i = 0; i < normalized.length(); i += 2) {
            out[i / 2] = (byte) Integer.parseInt(normalized.substring(i, i + 2), 16);
        }
        return out;
    }
}
