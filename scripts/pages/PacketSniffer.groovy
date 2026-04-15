import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.sokybot.machinepages.api.BasePage
import org.sokybot.network.IPacketObserver
import org.sokybot.network.IPacketPublisher
import org.sokybot.network.IPacketSubscription
import org.sokybot.network.NetworkPeer
import org.sokybot.network.packet.ImmutablePacket
import org.sokybot.network.packet.MutablePacket
import org.sokybot.proxy.IProxyConnection
import org.sokybot.proxy.recording.IPacketRecorder
import org.sokybot.proxy.recording.PacketDirection
import org.sokybot.proxy.recording.PacketRecording
import org.sokybot.proxy.recording.RecordedPacket
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import java.io.FilenameFilter

class TracerModel {
    NetworkPeer source
    String name
    boolean ignored
    String id
    int opcode
    int count
    String description
}

class SnifferPacket {
    String name
    ImmutablePacket packet
    int index
}

class StructField {
    String name
    String type
    int offset
    int length
    String comment
    String endian = "little"
}

class StructDefinition {
    int opcode
    List<StructField> fields = []
}

class PacketVar {
    String packet
    String varName
    String formatedValue
    String originValue
    String comment
}

class PacketFilter {
    interface Pred {
        boolean test(Map<String, Object> row)
    }

    static Pred parse(String query) {
        if (!query || !query.trim()) {
            return { Map<String, Object> row -> true } as Pred
        }
        List<String> tokens = tokenize(query)
        return new Parser(tokens).parseExpression()
    }

    static String supportedSyntaxHint() {
        return [
            "opcode:0x5000",
            "opcode:0x30*",
            "source:SERVER",
            "name:AUTH",
            "size:>20",
            "encoding:PLAIN",
            "payload_contains:\"0F 3A\"",
            "time:>10:30:00",
            "payload_regex:\"[0-9A-F]{4}00\"",
            "Use AND/OR/NOT with parentheses"
        ].join(", ")
    }

    private static List<String> tokenize(String query) {
        List<String> tokens = []
        StringBuilder current = new StringBuilder()
        boolean inQuotes = false
        for (char c : query.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes
                current.append(c)
                continue
            }
            if (!inQuotes && (c == '(' || c == ')')) {
                flush(current, tokens)
                tokens.add(String.valueOf(c))
                continue
            }
            if (!inQuotes && Character.isWhitespace(c)) {
                flush(current, tokens)
                continue
            }
            current.append(c)
        }
        flush(current, tokens)
        return tokens
    }

    private static void flush(StringBuilder current, List<String> tokens) {
        if (current.length() > 0) {
            tokens.add(current.toString())
            current.setLength(0)
        }
    }

    private static String stringify(Object v) { v == null ? "" : String.valueOf(v) }

    private static int parseHexOrDec(String value) {
        String v = stringify(value).trim().toLowerCase(Locale.ROOT)
        if (!v) return 0
        if (v.startsWith("0x")) return Integer.parseInt(v.substring(2), 16)
        return Integer.parseInt(v)
    }

    private static String stripQuotes(String value) {
        if (!value) return ""
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1)
        }
        return value
    }

    private static class Parser {
        private final List<String> tokens
        private int pos = 0

        Parser(List<String> tokens) {
            this.tokens = tokens
        }

        Pred parseExpression() {
            Pred left = parseTerm()
            while (match("OR")) {
                Pred right = parseTerm()
                left = { Map<String, Object> row -> left.test(row) || right.test(row) } as Pred
            }
            return left
        }

        Pred parseTerm() {
            Pred left = parseFactor()
            while (match("AND")) {
                Pred right = parseFactor()
                left = { Map<String, Object> row -> left.test(row) && right.test(row) } as Pred
            }
            return left
        }

        Pred parseFactor() {
            if (match("NOT")) {
                Pred nested = parseFactor()
                return { Map<String, Object> row -> !nested.test(row) } as Pred
            }
            if (match("(")) {
                Pred nested = parseExpression()
                expect(")")
                return nested
            }
            String token = next()
            if (!token) return { Map<String, Object> row -> true } as Pred
            if (token.startsWith("-")) {
                Pred p = parseClause(token.substring(1))
                return { Map<String, Object> row -> !p.test(row) } as Pred
            }
            return parseClause(token)
        }

        private Pred parseClause(String token) {
            int idx = token.indexOf(":")
            if (idx <= 0) {
                String needle = token.toLowerCase(Locale.ROOT)
                return { Map<String, Object> row ->
                    stringify(row.name).toLowerCase(Locale.ROOT).contains(needle)
                } as Pred
            }
            String field = token.substring(0, idx).toLowerCase(Locale.ROOT)
            String rawValue = stripQuotes(token.substring(idx + 1))
            switch (field) {
                case "opcode":
                    return opcodePredicate(rawValue)
                case "source":
                    return { Map<String, Object> row -> stringify(row.source).equalsIgnoreCase(rawValue) } as Pred
                case "name":
                    return { Map<String, Object> row ->
                        stringify(row.name).toLowerCase(Locale.ROOT).contains(rawValue.toLowerCase(Locale.ROOT))
                    } as Pred
                case "size":
                    return sizePredicate(rawValue)
                case "encoding":
                    return { Map<String, Object> row -> stringify(row.encoding).equalsIgnoreCase(rawValue) } as Pred
                case "payload_contains":
                    String normalized = rawValue.replaceAll("\\s+", "").toUpperCase(Locale.ROOT)
                    return { Map<String, Object> row ->
                        stringify(row.payload).replaceAll("\\s+", "").toUpperCase(Locale.ROOT).contains(normalized)
                    } as Pred
                case "payload_regex":
                    Pattern pattern = Pattern.compile(rawValue)
                    return { Map<String, Object> row -> pattern.matcher(stringify(row.payload)).find() } as Pred
                default:
                    return { Map<String, Object> row -> true } as Pred
            }
        }

        private Pred opcodePredicate(String value) {
            String normalized = value.toLowerCase(Locale.ROOT)
            if (normalized.endsWith("*")) {
                String prefix = normalized.substring(0, normalized.length() - 1).replace("0x", "")
                return { Map<String, Object> row ->
                    String op = stringify(row.opcode).toLowerCase(Locale.ROOT).replace("0x", "")
                    op.startsWith(prefix)
                } as Pred
            }
            int expected = parseHexOrDec(normalized)
            return { Map<String, Object> row -> parseHexOrDec(stringify(row.opcode)) == expected } as Pred
        }

        private Pred sizePredicate(String value) {
            String v = value.trim()
            String op = "="
            if (v.startsWith(">=") || v.startsWith("<=")) {
                op = v.substring(0, 2)
                v = v.substring(2)
            } else if (v.startsWith(">") || v.startsWith("<") || v.startsWith("=")) {
                op = v.substring(0, 1)
                v = v.substring(1)
            }
            int n = parseHexOrDec(v)
            switch (op) {
                case ">":
                    return { Map<String, Object> row -> parseHexOrDec(stringify(row.size)) > n } as Pred
                case "<":
                    return { Map<String, Object> row -> parseHexOrDec(stringify(row.size)) < n } as Pred
                case ">=":
                    return { Map<String, Object> row -> parseHexOrDec(stringify(row.size)) >= n } as Pred
                case "<=":
                    return { Map<String, Object> row -> parseHexOrDec(stringify(row.size)) <= n } as Pred
                default:
                    return { Map<String, Object> row -> parseHexOrDec(stringify(row.size)) == n } as Pred
            }
        }

        private boolean match(String token) {
            if (pos >= tokens.size()) return false
            if (!tokens[pos].equalsIgnoreCase(token)) return false
            pos++
            return true
        }

        private void expect(String token) {
            if (!match(token)) throw new IllegalArgumentException("Expected '${token}' in filter query")
        }

        private String next() {
            if (pos >= tokens.size()) return null
            return tokens[pos++]
        }
    }
}

// Use System.getProperties() as a JVM-global singleton to store state that survives script reloads.
// This avoids OSGi classloading issues entirely since System.getProperties() is always accessible.
import java.util.concurrent.ConcurrentHashMap

class PacketSnifferPage extends BasePage {
    private static final String GLOBAL_STORE_KEY = "__sokybot_page_state_store__"
    
    private static Map<String, Object> getGlobalStore() {
        def props = System.getProperties()
        synchronized (props) {
            def store = props.get(GLOBAL_STORE_KEY)
            if (store == null) {
                store = new ConcurrentHashMap<String, Object>()
                props.put(GLOBAL_STORE_KEY, store)
            }
            return store
        }
    }
    
    private String getMachineKey() { machineContext.fullName() }
    
    private List<SnifferPacket> getMonitorPackets() { getGlobalStore().computeIfAbsent(getMachineKey() + ".monitorPackets", k -> []) }
    private List<TracerModel> getTracers() { getGlobalStore().computeIfAbsent(getMachineKey() + ".tracers", k -> []) }
    private Map<String, TracerModel> getTracerCache() { getGlobalStore().computeIfAbsent(getMachineKey() + ".tracerCache", k -> [:]) }
    private Map<Integer, StructDefinition> getStructDefinitions() { getGlobalStore().computeIfAbsent(getMachineKey() + ".structDefinitions", k -> [:]) }
    private Map<String, Integer> getOpcodeFrequency() { getGlobalStore().computeIfAbsent(getMachineKey() + ".opcodeFrequency", k -> [:]) }
    private List<Integer> getSelectedPacketIndices() { getGlobalStore().computeIfAbsent(getMachineKey() + ".selectedPacketIndices", k -> []) }
    
    private boolean isMonitorEnabled() { getGlobalStore().get(getMachineKey() + ".monitorEnabled") != false }
    private void setMonitorEnabled(boolean v) { getGlobalStore().put(getMachineKey() + ".monitorEnabled", v) }
    private boolean isSubscribed() { getGlobalStore().get(getMachineKey() + ".subscribed") == true }
    private void setSubscribed(boolean v) { getGlobalStore().put(getMachineKey() + ".subscribed", v) }
    private boolean isTrafficAutoScroll() { getGlobalStore().get(getMachineKey() + ".trafficAutoScroll") != false }
    private void setTrafficAutoScroll(boolean v) { getGlobalStore().put(getMachineKey() + ".trafficAutoScroll", v) }


    private static final DateTimeFormatter PACKET_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray()
    private static final int MAX_CORRUPT_FILES = 5
    private static final long STATUS_HEARTBEAT_INTERVAL_MS = 30000L

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
    private final File tracerFile = new File("./packet-data.json")
    private final File structFile = new File("./struct-definitions.json")

    private final Object mutex = new Object()
    private final BlockingQueue<SnifferPacket> packetQueue = new ArrayBlockingQueue<>(10000)
    private final ScheduledExecutorService updateExecutor = Executors.newSingleThreadScheduledExecutor()
    private final AtomicBoolean updateScheduled = new AtomicBoolean(false)
    private final Sinks.Many<Map<String, Object>> packetStreamSink = Sinks.many().multicast().onBackpressureBuffer(1000)

    private IProxyConnection proxyConnection
    private IPacketRecorder packetRecorder
    private IPacketSubscription packetSubscription

    private String activeTab = "traffic"
    private String tracerSearchFilter = ""
    private String monitorFilterQuery = ""
    private String monitorFilterError = ""
    private PacketFilter.Pred monitorFilterPredicate = { Map<String, Object> row -> true } as PacketFilter.Pred
    private TracerModel selectedTracer
    private int packetCount = 0
    private int lastSentPacketCount = 0
    private long lastUpdateTime = 0L
    private volatile long lastPacketEmitTime = System.currentTimeMillis()
    private long lastStatusEmitTime = 0L
    private long totalPacketsSeen = 0L
    private long droppedIgnoredCount = 0L
    private long droppedFilterCount = 0L
    private String lastDropReason = ""
    private long lastDropTimestamp = 0L
    private long lastDisplayedPacketTimestamp = 0L
    private int subscriptionGeneration = 0
    private String bindingState = "unbound"
    private long lastBindAttemptAt = 0L
    private String lastBindError = ""
    private boolean isPublisherAvailable = false
    private String boundProxyIdentity = ""
    private final Map<String, Object> cachedState = [:]

    private boolean diffOpen = false
    private List<Map<String, Object>> diffA = []
    private List<Map<String, Object>> diffB = []
    private boolean replayOpen = false
    private String replayDirection = "C2S"
    private List<Map<String, Object>> replayPacketRows = []
    private boolean recordingActive = false
    private PacketRecording lastRecording
    private boolean analyzerOpen = false
    /** When set, hex viewer shows only this monitor list index; null = all filtered packets. */
    private Integer analyzerFocusListIndex = null
    private List<PacketVar> analyzerVars = []
    private String selectedHex = ""
    private int selectedByteCount = 0
    private int matchCount = 0
    private int analyzerGroupLen = 16

    PacketSnifferPage() {
        super("Packet Sniffer", "Activity")
    }

    @Override
    protected void setup() {
        loadPersistedData()
        packetRecorder = service(IPacketRecorder)
        proxyConnection = machineContext.getProxyConnection()
        println "[Groovy] setup: machine=${machineContext.fullName()}, proxyConnection=${proxyConnection}"
        startBatchedUpdateProcessor()
        ensurePacketSubscription()
    }

    private void loadPersistedData() {
        synchronized (mutex) {
            tracers.clear()
            tracers.addAll(safeReadList(tracerFile, new TypeReference<List<TracerModel>>() {}))
            tracerCache.clear()
            boolean tracerNamesChanged = normalizeTracerOpcodeNamesInPlace()
            tracers.each { t -> tracerCache[cacheKey(t.source, t.opcode)] = t }
            structDefinitions.clear()
            safeReadList(structFile, new TypeReference<List<StructDefinition>>() {}).each { d ->
                structDefinitions[d.opcode] = d
            }
            if (tracerNamesChanged) {
                try {
                    saveTracers()
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** Re-align persisted tracer labels with resolveOpcodeName (fixes stale names e.g. 0xA101 vs AGENT_LIST). */
    private boolean normalizeTracerOpcodeNamesInPlace() {
        boolean changed = false
        tracers.each { TracerModel t ->
            int op = t.opcode & 0xFFFF
            String resolved = resolveOpcodeName(op)
            if (resolved != "UNKNOWN" && t.name != resolved) {
                t.name = resolved
                changed = true
            }
        }
        return changed
    }

    private <T> List<T> safeReadList(File file, TypeReference<List<T>> typeRef) {
        if (file == null || !file.exists()) return []
        try {
            List<T> loaded = mapper.readValue(file, typeRef)
            return loaded ?: []
        } catch (Exception parseError) {
            println "[Groovy] persistence load_status=corrupt path=${file.absolutePath} parse_error=${parseError.message}"
            boolean quarantineSuccess = quarantineCorruptFile(file, parseError)
            if (!quarantineSuccess) {
                println "[Groovy] persistence quarantine_success=false path=${file.absolutePath} continuing_with_empty_state=true"
            }
            return []
        }
    }

    private boolean quarantineCorruptFile(File file, Exception parseError) {
        File parent = file.parentFile ?: new File(".")
        String ts = String.valueOf(System.currentTimeMillis())
        File target = new File(parent, file.name + ".corrupt." + ts)
        boolean moved = false
        try {
            Files.move(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            moved = true
            int pruned = pruneCorruptFiles(parent, file.name + ".corrupt.")
            println "[Groovy] persistence quarantine_attempted=true quarantine_success=true path=${file.absolutePath} quarantined=${target.absolutePath} quarantine_pruned_count=${pruned}"
        } catch (Exception moveError) {
            println "[Groovy] persistence quarantine_attempted=true quarantine_success=false path=${file.absolutePath} io_error=${moveError.message} parse_error=${parseError.message}"
        }
        return moved
    }

    private int pruneCorruptFiles(File dir, String prefix) {
        try {
            File[] files = dir.listFiles({ d, name -> name.startsWith(prefix) } as FilenameFilter)
            if (!files || files.length <= MAX_CORRUPT_FILES) return 0
            List<File> sorted = files.toList().sort { a, b -> Long.compare(a.lastModified(), b.lastModified()) }
            int removeCount = sorted.size() - MAX_CORRUPT_FILES
            int deleted = 0
            for (int i = 0; i < removeCount; i++) {
                try {
                    if (sorted[i].delete()) deleted++
                } catch (Exception ignored) {}
            }
            return deleted
        } catch (Exception e) {
            println "[Groovy] persistence quarantine_prune_failed io_error=${e.message}"
            return 0
        }
    }

    private String connectionIdentity(IProxyConnection conn) {
        if (!conn) return ""
        return conn.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(conn))
    }

    private Map<String, Object> ensurePacketSubscription() {
        synchronized (mutex) {
            lastBindAttemptAt = System.currentTimeMillis()
            proxyConnection = machineContext.getProxyConnection()
            if (!proxyConnection) {
                bindingState = "unbound"
                isPublisherAvailable = false
                setSubscribed(false)
                lastBindError = "Proxy connection unavailable"
                emitStatus(true)
                return [success: false, code: "proxy_missing", message: lastBindError]
            }
            IPacketPublisher publisher = proxyConnection.getPacketPublisher()
            if (!publisher) {
                bindingState = "bound_unsubscribed"
                isPublisherAvailable = false
                setSubscribed(false)
                lastBindError = "Packet publisher unavailable"
                emitStatus(true)
                return [success: false, code: "publisher_missing", message: lastBindError]
            }
            isPublisherAvailable = true
            String currentIdentity = connectionIdentity(proxyConnection)
            if (packetSubscription && isSubscribed() && boundProxyIdentity == currentIdentity) {
                bindingState = "subscribed"
                lastBindError = ""
                emitStatus(true)
                return [success: true, code: "already_bound", message: "Already bound to packet publisher"]
            }
            if (packetSubscription) {
                try { packetSubscription.unsubscribe() } catch (Exception ignored) {}
                packetSubscription = null
            }
            setSubscribed(false)
            bindingState = "binding"
            lastBindError = ""

            ClassLoader scriptClassLoader = this.getClass().getClassLoader()
            println "[Groovy] subscribeToPackets: subscribing to all packets for ${machineContext.fullName()}"
            IPacketObserver observer = [onPacket: { ImmutablePacket packet ->
                ClassLoader prev = Thread.currentThread().getContextClassLoader()
                try {
                    Thread.currentThread().setContextClassLoader(scriptClassLoader)
                    tryRecord(packet)
                    display(packet)
                } catch (Exception e) {
                    println "[Groovy] onPacket ERROR: ${e.message}"
                    e.printStackTrace()
                } finally {
                    Thread.currentThread().setContextClassLoader(prev)
                }
            }] as IPacketObserver
            try {
                packetSubscription = publisher.subscribeAll(observer)
                setSubscribed(true)
                bindingState = "subscribed"
                boundProxyIdentity = currentIdentity
                subscriptionGeneration++
                emitStatus(true)
                println "[Groovy] subscribeToPackets: subscription created: ${packetSubscription}, generation=${subscriptionGeneration}"
                return [success: true, code: "bound", message: "Packet stream bound", subscriptionGeneration: subscriptionGeneration]
            } catch (Exception e) {
                setSubscribed(false)
                bindingState = "error"
                lastBindError = e.message ?: "Subscription failed"
                emitStatus(true)
                return [success: false, code: "error", message: lastBindError]
            }
        }
    }

    private void refreshBindingState() {
        synchronized (mutex) {
            IProxyConnection current = machineContext.getProxyConnection()
            String currentIdentity = connectionIdentity(current)
            if (!current) {
                if (isSubscribed()) {
                    try { packetSubscription?.unsubscribe() } catch (Exception ignored) {}
                    packetSubscription = null
                }
                setSubscribed(false)
                isPublisherAvailable = false
                bindingState = "unbound"
                boundProxyIdentity = ""
                return
            }
            IPacketPublisher publisher = current.getPacketPublisher()
            if (!publisher) {
                if (isSubscribed()) {
                    try { packetSubscription?.unsubscribe() } catch (Exception ignored) {}
                    packetSubscription = null
                }
                setSubscribed(false)
                isPublisherAvailable = false
                bindingState = "bound_unsubscribed"
                boundProxyIdentity = currentIdentity
                return
            }
            isPublisherAvailable = true
            if (isSubscribed() && boundProxyIdentity == currentIdentity) {
                bindingState = "subscribed"
                return
            }
            if (!isSubscribed()) {
                bindingState = "bound_unsubscribed"
                boundProxyIdentity = currentIdentity
            }
        }
    }

    private Map<String, Object> buildStatusPayload() {
        return [
            type                : "STATUS",
            isProxyBound        : machineContext.getProxyConnection() != null,
            isPublisherAvailable: isPublisherAvailable,
            isSubscribed        : isSubscribed(),
            subscriptionGeneration: subscriptionGeneration,
            bindingState        : bindingState,
            lastBindAttemptAt   : lastBindAttemptAt,
            lastBindError       : lastBindError,
            totalPacketsSeen    : totalPacketsSeen,
            droppedIgnoredCount : droppedIgnoredCount,
            droppedFilterCount  : droppedFilterCount,
            lastDropReason      : lastDropReason,
            lastDropTimestamp   : lastDropTimestamp,
            lastDisplayedPacketTimestamp: lastDisplayedPacketTimestamp,
            timestamp           : System.currentTimeMillis()
        ]
    }

    private void emitStatus(boolean force = false) {
        long now = System.currentTimeMillis()
        if (!force && now - lastStatusEmitTime < STATUS_HEARTBEAT_INTERVAL_MS) return
        packetStreamSink.tryEmitNext(buildStatusPayload())
        lastStatusEmitTime = now
    }

    private void tryRecord(ImmutablePacket packet) {
        if (!packetRecorder) return
        String machine = machineContext.fullName()
        if (!packetRecorder.isRecording(machine)) return
        PacketDirection direction = packet.packetSource == NetworkPeer.SERVER ? PacketDirection.TO_CLIENT : PacketDirection.TO_SERVER
        packetRecorder.recordPacket(machine, RecordedPacket.now(direction, packet.opcode, packet.toBytes()))
    }

    private void startBatchedUpdateProcessor() {
        updateExecutor.scheduleAtFixedRate({
            if (!packetQueue.isEmpty() && updateScheduled.compareAndSet(false, true)) {
                processBatchedPackets()
            }
        }, 100, 100, TimeUnit.MILLISECONDS)
    }

    private void processBatchedPackets() {
        try {
            List<SnifferPacket> batch = []
            packetQueue.drainTo(batch, 50)
            if (!batch.isEmpty()) {
                synchronized (mutex) {
                    getMonitorPackets().addAll(batch)
                    packetCount = getMonitorPackets().size()
                    if (packetCount - lastSentPacketCount > 10 || System.currentTimeMillis() - lastUpdateTime > 500) {
                        notifyStateDelta()
                        lastSentPacketCount = packetCount
                        lastUpdateTime = System.currentTimeMillis()
                    }
                }
            }
        } finally {
            updateScheduled.set(false)
        }
    }

    void display(ImmutablePacket packet) {
        println "[Groovy] display: packet incoming for ${machineContext.fullName()} (monitorEnabled=${isMonitorEnabled()}), opcode=0x${Integer.toHexString(packet.opcode)}"
        synchronized (mutex) {
            totalPacketsSeen++
        }
        if (!isMonitorEnabled()) return
        synchronized (mutex) {
            int opcode = packet.opcode
            NetworkPeer source = packet.packetSource
            TracerModel tracer = getTracerCache()[cacheKey(source, opcode)]
            String frequencyKey = source.toString() + ":" + String.format("0x%04X", opcode & 0xFFFF)
            getOpcodeFrequency()[frequencyKey] = (getOpcodeFrequency()[frequencyKey] ?: 0) + 1

            if (!tracer) {
                tracer = new TracerModel(source: source, opcode: opcode, ignored: false, name: resolveOpcodeName(opcode), count: 1, id: source.name() + "." + Integer.toHexString(opcode))
                getTracers() << tracer
                getTracerCache()[cacheKey(source, opcode)] = tracer
                updateExecutor.execute({ saveTracers() })
            } else {
                tracer.count = tracer.count + 1
            }

            if (tracer.ignored) {
                droppedIgnoredCount++
                lastDropReason = "ignored"
                lastDropTimestamp = System.currentTimeMillis()
                return
            }
            SnifferPacket tablePacket = new SnifferPacket(name: tracer.name, packet: packet, index: getMonitorPackets().size())
            Map<String, Object> row = buildPacketRow(packet, tracer.name, tablePacket.index)
            row.subscriptionGeneration = subscriptionGeneration
            if (!matchesCurrentFilter(row)) {
                droppedFilterCount++
                lastDropReason = "filter"
                lastDropTimestamp = System.currentTimeMillis()
                return
            }
            // Keep analyzer and action handlers consistent with visible traffic rows.
            // Streaming alone is not enough because analyzer reads from monitorPackets.
            getMonitorPackets().add(tablePacket)
            packetCount = getMonitorPackets().size()
            packetStreamSink.tryEmitNext(row)
            lastPacketEmitTime = System.currentTimeMillis()
            lastDisplayedPacketTimestamp = lastPacketEmitTime
        }
    }

    @Override
    Map<String, Object> getInitialState() {
        // Always return plain page state. Returning wrapped cached payloads can
        // break declarative action/state merging in the frontend.
        return getCurrentState()
    }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        Map<String, Object> input = data ?: [:]
        try {
            Map<String, Object> response = [:]
            synchronized (mutex) {
                switch (action) {
                case "switchTab":
                    String tab = str(input.tab)
                    if (tab in ["traffic", "tracer"]) {
                        activeTab = tab
                        response.success = true
                    }
                    break
                case "clearMonitor":
                    getMonitorPackets().clear()
                    packetQueue.clear()
                    getOpcodeFrequency().clear()
                    packetCount = 0
                    lastSentPacketCount = 0
                    droppedFilterCount = 0L
                    droppedIgnoredCount = 0L
                    lastDropReason = ""
                    lastDropTimestamp = 0L
                    getSelectedPacketIndices().clear()
                    response.success = true
                    break
                case "toggleTrafficAutoScroll":
                    setTrafficAutoScroll(!isTrafficAutoScroll())
                    response.success = true
                    break
                case "togglePause":
                    setMonitorEnabled(!isMonitorEnabled())
                    response.success = true
                    break
                case "ensurePacketSubscription":
                    response.putAll(ensurePacketSubscription())
                    break
                case "openAnalyzer":
                    // Ensure analyzer sees the latest packets even if they are still queued.
                    flushQueuedPackets()
                    analyzerFocusListIndex = null
                    if (input.containsKey("index")) {
                        int idx = num(input.index, -1)
                        if (idx >= 0 && idx < getMonitorPackets().size()) {
                            analyzerFocusListIndex = idx
                        }
                    }
                    analyzerOpen = true
                    response.success = true
                    if (analyzerPackets().isEmpty()) {
                        response.info = "No packets available yet"
                    }
                    break
                case "closeAnalyzer":
                    analyzerOpen = false
                    analyzerFocusListIndex = null
                    response.success = true
                    break
                case "filterTracer":
                    tracerSearchFilter = str(input.value)
                    response.success = true
                    break
                case "selectTracer":
                    selectedTracer = getTracerCache()[cacheKey(parseNetworkPeer(str(input.source)), parseOpcode(str(input.opcode)))]
                    response.success = true
                    break
                case "focusTracer":
                    NetworkPeer src = parseNetworkPeer(str(input.source))
                    int opc = parseOpcode(str(input.opcode))
                    monitorFilterQuery = String.format("opcode:0x%04X AND source:%s", opc & 0xFFFF, src)
                    try {
                        monitorFilterPredicate = PacketFilter.parse(monitorFilterQuery)
                        monitorFilterError = ""
                    } catch (Exception e) {
                        monitorFilterError = e.message
                    }
                    activeTab = "traffic"
                    response.success = true
                    break
                case "updateDescription":
                    if (selectedTracer) {
                        selectedTracer.description = str(input.value)
                        saveTracers()
                        response.success = true
                    }
                    break
                case "saveDescription":
                    if (selectedTracer) {
                        saveTracers()
                        response.success = true
                    }
                    break
                case "updateTracerName":
                    TracerModel tr = getTracerCache()[cacheKey(parseNetworkPeer(str(input.source)), parseOpcode(str(input.opcode)))]
                    if (tr) {
                        tr.name = str(input.name)
                        saveTracers()
                        response.success = true
                    }
                    break
                case "ignore":
                    setIgnored(input, true)
                    response.success = true
                    break
                case "toggleIgnore":
                    setIgnored(input, null)
                    response.success = true
                    break
                case "filterMonitor":
                    monitorFilterQuery = str(input.value).trim()
                    try {
                        monitorFilterPredicate = PacketFilter.parse(monitorFilterQuery)
                        monitorFilterError = ""
                        if (!monitorFilterQuery) {
                            droppedFilterCount = 0L
                            if (lastDropReason == "filter") {
                                lastDropReason = ""
                                lastDropTimestamp = 0L
                            }
                        }
                        response.success = true
                    } catch (Exception e) {
                        monitorFilterError = e.message
                        response.success = false
                        response.error = monitorFilterError
                    }
                    break
                case "unignoreAllTracers":
                    getTracers().each { t -> t.ignored = false }
                    saveTracers()
                    droppedIgnoredCount = 0L
                    if (lastDropReason == "ignored") {
                        lastDropReason = ""
                        lastDropTimestamp = 0L
                    }
                    response.success = true
                    response.code = "unignored"
                    break
                case "selectPackets":
                    getSelectedPacketIndices().clear()
                    ((input.indices instanceof List) ? (List) input.indices : []).each { v ->
                        int idx = num(v, -1)
                        if (idx >= 0 && idx < getMonitorPackets().size()) getSelectedPacketIndices() << idx
                    }
                    response.success = true
                    break
                case "diffPackets":
                    response.putAll(handleDiffPackets(input))
                    break
                case "closeDiff":
                    diffOpen = false
                    diffA = []
                    diffB = []
                    response.success = true
                    break
                case "openReplay":
                    response.putAll(handleOpenReplay(input))
                    break
                case "closeReplay":
                    replayOpen = false
                    replayPacketRows = []
                    response.success = true
                    break
                case "defineField":
                    response.putAll(handleDefineField(input))
                    break
                case "updateField":
                    response.putAll(handleUpdateField(input))
                    break
                case "removeField":
                    response.putAll(handleRemoveField(input))
                    break
                case "listStructDefinitions":
                    response.success = true
                    response.structDefinitions = convertStructDefinitions()
                    break
                case "toggleRecording":
                    response.putAll(handleToggleRecording())
                    break
                case "exportSelected":
                    response.putAll(handleExportSelected())
                    break
                case "exportRecording":
                    response.putAll(handleExportRecording())
                    break
                case "injectPacket":
                    response.putAll(handleInjectPacket(input))
                    break
                case "selectHex":
                    response.putAll(handleSelectHex(input))
                    break
                case "defineVariable":
                    response.putAll(handleDefineVariable(input))
                    break
                case "updateVariable":
                    response.putAll(handleUpdateVariable(input))
                    break
                case "selectVariable":
                    response.putAll(handleSelectVariable(input))
                    break
                case "setGroupLen":
                    analyzerGroupLen = Math.max(8, Math.min(32, num(input.groupLen, 16)))
                    response.success = true
                    break
                case "getData":
                    response.success = true
                    break
                case "analyzerAction":
                    response.putAll(routeAnalyzerAction(input))
                    break
                case "copyAsCode":
                case "copyHex":
                case "copyJson":
                case "exportMonitorCsv":
                    // Handled client-side in DeclarativeExtensionView.
                    response.success = true
                    break
                default:
                    response.success = false
                    response.error = "Unknown action: ${action}"
                }
                response.state = getCurrentState()
            }
            emitStateUpdate()
            return response
        } catch (Exception e) {
            log.error("PacketSniffer action failed: {}", action, e)
            return [success: false, error: e.message ?: "Action failed", state: getCurrentState()]
        }
    }

    private Map<String, Object> routeAnalyzerAction(Map<String, Object> input) {
        String analyzerAction = str(input.action)
        Map<String, Object> payload = (Map<String, Object>) (input.data ?: [:])
        switch (analyzerAction) {
            case "selectHex": return handleSelectHex(payload)
            case "defineVariable": return handleDefineVariable(payload)
            case "updateVariable": return handleUpdateVariable(payload)
            case "selectVariable": return handleSelectVariable(payload)
            case "setGroupLen":
                analyzerGroupLen = Math.max(8, Math.min(32, num(payload.groupLen, 16)))
                return [success: true]
            case "getData":
                return [success: true]
            default:
                return [success: false, error: "Unknown analyzer action: ${analyzerAction}"]
        }
    }

    private Map<String, Object> handleSelectHex(Map<String, Object> input) {
        selectedHex = str(input.hex).trim()
        if (!selectedHex || !(selectedHex ==~ /^(?:([A-Fa-f0-9]{2})\s*)+$/)) {
            selectedByteCount = 0
            matchCount = 0
        } else {
            selectedByteCount = selectedHex.replaceAll("\\s+", "").length() / 2
            matchCount = findMatches(selectedHex)
        }
        return [success: true, selectedHex: selectedHex, selectedByteCount: selectedByteCount, matchCount: matchCount, matches: findMatchPositions(selectedHex)]
    }

    private Map<String, Object> handleDefineVariable(Map<String, Object> input) {
        String hex = str(input.hex)
        if (!hex || !(hex ==~ /^(?:([A-Fa-f0-9]{2})\s*)+$/)) {
            return [success: false, error: "Invalid hex string"]
        }
        analyzerVars << new PacketVar(packet: str(input.packetName ?: "Unknown"), varName: str(input.varName ?: "Undefined"), originValue: hex.trim(), formatedValue: "0x" + hex.replaceAll("\\s+", ""), comment: "")
        return [success: true]
    }

    private Map<String, Object> handleUpdateVariable(Map<String, Object> input) {
        int idx = num(input.index, -1)
        if (idx < 0 || idx >= analyzerVars.size()) return [success: false, error: "Invalid variable index"]
        if (input.containsKey("varName")) analyzerVars[idx].varName = str(input.varName)
        if (input.containsKey("comment")) analyzerVars[idx].comment = str(input.comment)
        return [success: true]
    }

    private Map<String, Object> handleSelectVariable(Map<String, Object> input) {
        int idx = num(input.index, -1)
        if (idx < 0 || idx >= analyzerVars.size()) return [success: false, error: "Invalid variable index"]
        return handleSelectHex([hex: analyzerVars[idx].originValue])
    }

    @Override
    Flux<Map<String, Object>> streamData(String streamId, Map<String, Object> params) {
        String id = streamId ?: ""
        if (id == "packets" || params?.get("stream") == "packets") {
            return streamPackets(params ?: [:])
        }
        if (id == "statistics" || params?.get("stream") == "statistics") {
            return streamStatistics(params ?: [:])
        }
        return super.streamData(streamId, params)
    }

    private Flux<Map<String, Object>> streamPackets(Map<String, Object> params) {
        String filter = str(params.filter)
        boolean includeHistory = params.includeHistory as Boolean ?: false
        Flux<Map<String, Object>> stream = packetStreamSink.asFlux().filter { matchesCurrentFilter(it) }
        Flux<Map<String, Object>> heartbeat = Flux.interval(java.time.Duration.ofMillis(STATUS_HEARTBEAT_INTERVAL_MS))
                .map {
                    refreshBindingState()
                    return buildStatusPayload()
                }
                .filter { it != null }
        if (filter) {
            String needle = filter.toLowerCase(Locale.ROOT)
            stream = stream.filter { Map<String, Object> row -> str(row.name).toLowerCase(Locale.ROOT).contains(needle) }
        }
        Flux<Map<String, Object>> initialStatus = Flux.defer {
            refreshBindingState()
            Flux.just(buildStatusPayload())
        }
        stream = Flux.concat(initialStatus, Flux.merge(stream, heartbeat))
        if (includeHistory) {
            return Flux.fromIterable(getRecentPackets(100)).concatWith(stream)
        }
        return stream
    }

    private Flux<Map<String, Object>> streamStatistics(Map<String, Object> params) {
        return Flux.interval(java.time.Duration.ofSeconds(1)).map {
            synchronized (mutex) {
                [packetCount: getMonitorPackets().size(), tracerCount: getTracers().size(), queueSize: packetQueue.size(), monitorEnabled: isMonitorEnabled(), timestamp: System.currentTimeMillis()]
            }
        }
    }

    private List<Map<String, Object>> getRecentPackets(int count) {
        synchronized (mutex) {
            List<SnifferPacket> filtered = getFilteredMonitorPackets()
            int start = Math.max(0, filtered.size() - count)
            return convertPacketsToData(filtered.subList(start, filtered.size()))
        }
    }

    private List<SnifferPacket> getFilteredMonitorPackets() {
        if (!monitorFilterQuery?.trim()) return new ArrayList<>(getMonitorPackets())
        return getMonitorPackets().findAll { p -> matchesCurrentFilter(buildPacketRow(p.packet, p.name, p.index)) }
    }

    private boolean matchesCurrentFilter(Map<String, Object> row) {
        if (row?.type == "STATUS" || row?.type == "STREAM_TERMINATED") return true
        try {
            return monitorFilterPredicate == null || monitorFilterPredicate.test(row)
        } catch (Exception e) {
            monitorFilterError = "Filter runtime error: " + e.message
            return true
        }
    }

    private List<TracerModel> getFilteredTracers() {
        if (!tracerSearchFilter?.trim()) return new ArrayList<>(getTracers())
        String filter = tracerSearchFilter.toLowerCase(Locale.ROOT)
        return getTracers().findAll { t ->
            str(t.name).toLowerCase(Locale.ROOT).contains(filter) ||
                Integer.toHexString(t.opcode).contains(filter) ||
                str(t.source).toLowerCase(Locale.ROOT).contains(filter)
        }
    }

    private Map<String, Object> getCurrentState() {
        int serverCount = 0
        int clientCount = 0
        int botCount = 0
        getMonitorPackets().each { p ->
            if (p?.packet?.packetSource == NetworkPeer.SERVER) serverCount++
            if (p?.packet?.packetSource == NetworkPeer.CLIENT) clientCount++
            if (p?.packet?.packetSource == NetworkPeer.BOT) botCount++
        }
        Map<String, Object> state = [
            activeTab                : activeTab,
            monitorEnabled           : isMonitorEnabled(),
            isProxyBound             : machineContext.getProxyConnection() != null,
            isPublisherAvailable     : isPublisherAvailable,
            isSubscribed             : isSubscribed(),
            subscriptionGeneration   : subscriptionGeneration,
            bindingState             : bindingState,
            lastBindAttemptAt        : lastBindAttemptAt,
            lastBindError            : lastBindError,
            tracerSearchFilter       : tracerSearchFilter,
            monitorFilterQuery       : monitorFilterQuery,
            monitorFilterError       : monitorFilterError,
            monitorFilterHint        : PacketFilter.supportedSyntaxHint(),
            packetCount              : packetCount,
            totalPacketsSeen         : totalPacketsSeen,
            droppedIgnoredCount      : droppedIgnoredCount,
            droppedFilterCount       : droppedFilterCount,
            lastDropReason           : lastDropReason,
            lastDropTimestamp        : lastDropTimestamp,
            lastDisplayedPacketTimestamp: lastDisplayedPacketTimestamp,
            serverCount              : serverCount,
            clientCount              : clientCount,
            botCount                 : botCount,
            topOpcodes               : getTopOpcodes(),
            monitorIcon              : isMonitorEnabled() ? "Pause" : "Play",
            monitorActionText        : isMonitorEnabled() ? "Pause" : "Resume",
            trafficAutoScroll        : isTrafficAutoScroll(),
            selectedTracer           : selectedTracer ? [source: selectedTracer.source.toString(), opcode: selectedTracer.opcode, description: str(selectedTracer.description)] : [:],
            trafficPackets           : convertPacketsToData(getFilteredMonitorPackets()),
            packetTracers            : convertTracersToData(getTracers()),
            filteredTracers          : convertTracersToData(getFilteredTracers()),
            selectedPacketIndices    : new ArrayList<>(getSelectedPacketIndices()),
            diffOpen                 : diffOpen,
            diffPacketsA             : diffA,
            diffPacketsB             : diffB,
            recordingActive          : recordingActive,
            structDefinitions        : convertStructDefinitions(),
            replayOpen               : replayOpen,
            replayDirection          : replayDirection,
            replayPacketRows         : replayPacketRows,
            selectedTracerDescription: selectedTracer ? str(selectedTracer.description) : "",
            analyzerOpen             : analyzerOpen,
            selectedHex              : selectedHex,
            selectedByteCount        : selectedByteCount,
            matchCount               : matchCount,
            variables                : toVarRows(),
            packets                  : analyzerPackets(),
            decodedFields            : getLatestDecodedFields()
        ]
        return state
    }

    private void notifyStateDelta() {
        Map<String, Object> current = getCurrentState()
        Map<String, Object> delta = [:]
        if (current.packetCount != cachedState.packetCount) {
            delta.packetCount = current.packetCount
            int newPackets = packetCount - lastSentPacketCount
            if (newPackets > 0 && newPackets < 100) {
                List<SnifferPacket> list = getMonitorPackets().subList(Math.max(0, getMonitorPackets().size() - newPackets), getMonitorPackets().size())
                delta.newPackets = convertPacketsToData(list)
            }
        }
        if (!delta.isEmpty()) {
            stateSink.tryEmitNext([delta: delta, timestamp: System.currentTimeMillis()])
        }
        cachedState.putAll(current)
    }

    private void flushQueuedPackets() {
        List<SnifferPacket> drained = []
        packetQueue.drainTo(drained)
        if (!drained.isEmpty()) {
            getMonitorPackets().addAll(drained)
            packetCount = getMonitorPackets().size()
        }
    }

    private List<Map<String, Object>> analyzerPackets() {
        List<SnifferPacket> packets
        if (analyzerFocusListIndex != null) {
            int i = analyzerFocusListIndex
            packets = (i >= 0 && i < getMonitorPackets().size()) ? [getMonitorPackets().get(i)] : []
        } else {
            packets = getFilteredMonitorPackets()
        }
        List<Map<String, Object>> rows = []
        packets.each { p ->
            byte[] buffer = p.packet.toBytes()
            rows << [type: "header", source: p.packet.packetSource.toString(), opcode: String.format("0x%04X", p.packet.opcode & 0xFFFF), name: p.name]
            int j = 0
            while (j < buffer.length) {
                rows << [type: "data", lineNumber: String.format("%06X", j), hex: toHex(buffer, j, analyzerGroupLen), ascii: toAscii(buffer, j, analyzerGroupLen), startOffset: j, endOffset: Math.min(j + analyzerGroupLen, buffer.length)]
                j += analyzerGroupLen
            }
            rows << [type: "separator"]
        }
        return rows
    }

    private List<Map<String, Object>> toVarRows() {
        List<Map<String, Object>> out = []
        analyzerVars.eachWithIndex { PacketVar v, int i ->
            out << [index: i, packet: v.packet, varName: v.varName, value: v.formatedValue, comment: v.comment, originValue: v.originValue]
        }
        return out
    }

    private int findMatches(String hex) {
        if (!hex) return 0
        String pattern = hex.replaceAll("\\s+", "").toUpperCase(Locale.ROOT)
        int count = 0
        getFilteredMonitorPackets().each { p ->
            String packetHex = bytesToHex(p.packet.toBytes())
            int idx = 0
            while ((idx = packetHex.indexOf(pattern, idx)) != -1) {
                count++
                idx += pattern.length()
            }
        }
        return count
    }

    private List<Map<String, Object>> findMatchPositions(String hex) {
        List<Map<String, Object>> out = []
        if (!hex) return out
        String pattern = hex.replaceAll("\\s+", "").toUpperCase(Locale.ROOT)
        getFilteredMonitorPackets().eachWithIndex { SnifferPacket p, int packetIdx ->
            String packetHex = bytesToHex(p.packet.toBytes())
            int idx = 0
            while ((idx = packetHex.indexOf(pattern, idx)) != -1) {
                out << [packetIndex: packetIdx, byteOffset: (idx / 2) as int, length: (pattern.length() / 2) as int]
                idx += pattern.length()
            }
        }
        return out
    }

    private List<Map<String, Object>> convertPacketsToData(List<SnifferPacket> packets) {
        packets.collect { p -> buildPacketRow(p.packet, p.name, p.index) }
    }

    private Map<String, Object> buildPacketRow(ImmutablePacket p, String name, int index) {
        long now = System.currentTimeMillis()
        [
            index       : index,
            source      : p.packetSource.toString(),
            encoding    : p.packetEncoding.toString(),
            size        : p.packetSize,
            name        : name,
            opcode      : "0x" + Integer.toHexString(p.opcode & 0xFFFF),
            count       : "0x" + Integer.toHexString(p.count & 0xFF),
            crc         : "0x" + Integer.toHexString(p.CRC & 0xFF),
            payload     : bytesToHex(p.toBytes()),
            opcodeValue : p.opcode,
            decodedFields: decodeStructFields(p),
            timestamp   : now,
            time        : LocalTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault()).format(PACKET_TIME_FORMATTER)
        ]
    }

    private List<Map<String, Object>> convertTracersToData(List<TracerModel> values) {
        values.collect { t ->
            [name: t.name, source: t.source.toString(), opcode: "0x" + Integer.toHexString(t.opcode & 0xFFFF), count: t.count, ignored: t.ignored, description: str(t.description)]
        }
    }

    private List<Map<String, Object>> getTopOpcodes() {
        getOpcodeFrequency().entrySet().toList().sort { a, b -> b.value <=> a.value }.take(10).collect { e ->
            String[] parts = e.key.split(":", 2)
            String source = parts.length > 0 ? parts[0] : "UNKNOWN"
            String opcode = parts.length > 1 ? parts[1] : "0x0000"
            [source: source, opcode: opcode, name: resolveOpcodeName(parseOpcode(opcode)), count: e.value]
        }
    }

    private static String resolveOpcodeName(int opcode) {
        return [
            (0x5000): "SETUP", (0x5001): "CHALLENGE", (0x2001): "MODULE_ID", (0x9000): "HANDSHAKE_ACCEPT",
            (0x2002): "PATCH_INFO", (0xA100): "AUTH_REQUEST", (0xA101): "AGENT_LIST", (0xA102): "LOGIN_RESPONSE",
            (0xA103): "AUTH_RESPONSE", (0x6100): "GATEWAY_CLIENT_BUILD", (0x6101): "AGENT_REQUEST", (0x6102): "LOGIN_REQUEST",
            (0x2322): "GATEWAY_IMAGE_CHALLENGE", (0x6323): "IMAGE_CODE_ANSWER", (0xA323): "IMAGE_CODE_RESULT",
            (0x600D): "MASSIVE", (0x34B5): "TELEPORT_COMPLETE",
            (0x3020): "CHARACTER_DATA", (0x3013): "ENTITY_SPAWN", (0x3015): "ENTITY_DESPAWN"
        ][opcode] ?: "UNKNOWN"
    }

    private void setIgnored(Map<String, Object> input, Boolean force) {
        TracerModel tr = getTracerCache()[cacheKey(parseNetworkPeer(str(input.source)), parseOpcode(str(input.opcode)))]
        if (!tr) return
        tr.ignored = (force == null) ? !tr.ignored : force
        saveTracers()
    }

    private Map<String, Object> handleDiffPackets(Map<String, Object> input) {
        println "[Groovy] handleDiffPackets: input=${input}, monitorPackets.size=${getMonitorPackets().size()}, selectedPacketIndices=${getSelectedPacketIndices()}"
        List<Integer> indices = []
        if (input.indices instanceof List && !((List) input.indices).isEmpty()) {
            indices = ((List) input.indices).collect { num(it, -1) }.findAll { it >= 0 }
        } else {
            indices = new ArrayList<>(getSelectedPacketIndices())
        }
        
        // Be forgiving: if we have fewer than 2, try to use whatever we have or fallback to last 2
        List<Integer> normalized = indices.findAll { it >= 0 && it < getMonitorPackets().size() }.unique().sort()
        
        println "[Groovy] handleDiffPackets: parsed indices=${indices}, normalized=${normalized}"

        if (normalized.size() < 2 && getMonitorPackets().size() >= 2) {
            normalized = [getMonitorPackets().size() - 2, getMonitorPackets().size() - 1]
            println "[Groovy] handleDiffPackets: fallback to last 2: ${normalized}"
        }
        
        if (normalized.size() < 2) {
            println "[Groovy] handleDiffPackets: failed - not enough packets"
            return [success: false, error: "Select at least 2 packets to compare"]
        }
        
        if (normalized.size() > 2) {
            normalized = normalized.takeRight(2)
        }
        
        SnifferPacket a = getMonitorPackets()[normalized[0]]
        SnifferPacket b = getMonitorPackets()[normalized[1]]
        List<Integer> changed = diffOffsets(a.packet.toBytes(), b.packet.toBytes())
        diffA = toHexRows(a, changed, groupLenFromData(input))
        diffB = toHexRows(b, changed, groupLenFromData(input))
        diffOpen = true
        [success: true]
    }

    private Map<String, Object> handleOpenReplay(Map<String, Object> input) {
        int index = num(input.index, -1)
        if (index < 0 || index >= getMonitorPackets().size()) return [success: false, error: "Invalid packet index"]
        SnifferPacket p = getMonitorPackets()[index]
        replayPacketRows = toHexRows(p, [], 16)
        replayDirection = p.packet.packetSource == NetworkPeer.SERVER ? "S2C" : "C2S"
        replayOpen = true
        [success: true]
    }

    private int groupLenFromData(Map<String, Object> input) {
        int g = num(input.groupLen, 16)
        Math.max(8, Math.min(32, g))
    }

    private List<Integer> diffOffsets(byte[] a, byte[] b) {
        List<Integer> out = []
        int max = Math.max(a.length, b.length)
        for (int i = 0; i < max; i++) {
            byte av = i < a.length ? a[i] : 0
            byte bv = i < b.length ? b[i] : 0
            if (i >= a.length || i >= b.length || av != bv) out << i
        }
        out
    }

    private List<Map<String, Object>> toHexRows(SnifferPacket p, List<Integer> changedOffsets, int groupLen) {
        List<Map<String, Object>> rows = []
        byte[] buffer = p.packet.toBytes()
        rows << [type: "header", source: p.packet.packetSource.toString(), opcode: String.format("0x%04X", p.packet.opcode & 0xFFFF), name: p.name]
        int i = 0
        while (i < buffer.length) {
            int start = i
            int end = Math.min(i + groupLen, buffer.length)
            rows << [type: "data", lineNumber: String.format("%06X", start), hex: toHexLine(buffer, start, end), ascii: toAsciiLine(buffer, start, end), startOffset: start, endOffset: end, changedOffsets: changedOffsets.findAll { v -> v >= start && v < end }]
            i += groupLen
        }
        rows << [type: "separator"]
        rows
    }

    private String toHexLine(byte[] bytes, int start, int end) {
        StringBuilder sb = new StringBuilder()
        for (int i = start; i < end; i++) {
            if (sb.length() > 0) sb.append(" ")
            sb.append(String.format("%02X", bytes[i]))
        }
        sb.toString()
    }

    private String toAsciiLine(byte[] bytes, int start, int end) {
        StringBuilder sb = new StringBuilder()
        for (int i = start; i < end; i++) {
            int c = bytes[i] & 0xFF
            sb.append(c >= 32 && c <= 126 ? (char) c : ".")
        }
        sb.toString()
    }

    private Map<String, Object> handleDefineField(Map<String, Object> input) {
        int opcode = parseOpcode(str(input.opcode ?: "0"))
        StructDefinition definition = getStructDefinitions()[opcode]
        if (!definition) {
            definition = new StructDefinition(opcode: opcode, fields: [])
            getStructDefinitions()[opcode] = definition
        }
        definition.fields << new StructField(name: str(input.name ?: "field_${opcode}"), type: str(input.type ?: "bytes"), offset: num(input.offset, 0), length: Math.max(1, num(input.length, 1)), comment: str(input.comment), endian: str(input.endian ?: "little"))
        persistStructDefinitions()
        [success: true]
    }

    private Map<String, Object> handleUpdateField(Map<String, Object> input) {
        int opcode = parseOpcode(str(input.opcode ?: "0"))
        int index = num(input.index, -1)
        StructDefinition definition = getStructDefinitions()[opcode]
        if (!definition || index < 0 || index >= definition.fields.size()) return [success: false, error: "Field not found"]
        StructField f = definition.fields[index]
        if (input.containsKey("name")) f.name = str(input.name)
        if (input.containsKey("type")) f.type = str(input.type)
        if (input.containsKey("offset")) f.offset = num(input.offset, f.offset)
        if (input.containsKey("length")) f.length = Math.max(1, num(input.length, f.length))
        if (input.containsKey("comment")) f.comment = str(input.comment)
        if (input.containsKey("endian")) f.endian = str(input.endian)
        persistStructDefinitions()
        [success: true]
    }

    private Map<String, Object> handleRemoveField(Map<String, Object> input) {
        int opcode = parseOpcode(str(input.opcode ?: "0"))
        int index = num(input.index, -1)
        StructDefinition definition = getStructDefinitions()[opcode]
        if (!definition || index < 0 || index >= definition.fields.size()) return [success: false, error: "Field not found"]
        definition.fields.remove(index)
        persistStructDefinitions()
        [success: true]
    }

    private void persistStructDefinitions() {
        mapper.writeValue(structFile, new ArrayList<>(getStructDefinitions().values()))
    }

    private List<Map<String, Object>> convertStructDefinitions() {
        getStructDefinitions().values().collect { definition ->
            [opcode: String.format("0x%04X", definition.opcode & 0xFFFF), opcodeValue: definition.opcode, fields: definition.fields.collectWithIndex { StructField f, int i ->
                [index: i, name: f.name, type: f.type, offset: f.offset, length: f.length, comment: str(f.comment), endian: str(f.endian ?: "little")]
            }]
        }
    }

    private List<Map<String, Object>> decodeStructFields(ImmutablePacket packet) {
        StructDefinition definition = getStructDefinitions()[packet.opcode]
        if (!definition || definition.fields.isEmpty()) return []
        byte[] payload = packet.toBytes()
        List<Map<String, Object>> out = []
        definition.fields.each { f ->
            int start = Math.max(0, f.offset)
            int end = Math.min(payload.length, start + Math.max(1, f.length))
            if (start >= end) return
            byte[] raw = Arrays.copyOfRange(payload, start, end)
            out << [name: f.name, type: f.type, offset: f.offset, length: f.length, hexValue: bytesToHex(raw), decodedValue: decodeFieldValue(raw, f)]
        }
        out
    }

    private String decodeFieldValue(byte[] raw, StructField field) {
        String type = str(field.type ?: "bytes").toLowerCase(Locale.ROOT)
        ByteOrder order = "big".equalsIgnoreCase(str(field.endian)) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN
        ByteBuffer buffer = ByteBuffer.wrap(Arrays.copyOf(raw, Math.max(raw.length, 8))).order(order)
        switch (type) {
            case "uint8": return String.valueOf(raw[0] & 0xFF)
            case "uint16": return String.valueOf(buffer.getShort(0) & 0xFFFF)
            case "uint32": return String.valueOf(buffer.getInt(0) & 0xFFFFFFFFL)
            case "int16": return String.valueOf(buffer.getShort(0))
            case "int32": return String.valueOf(buffer.getInt(0))
            case "float": return String.valueOf(buffer.getFloat(0))
            case "string": return new String(raw, StandardCharsets.UTF_8).replace("\u0000", "")
            default: return bytesToHex(raw)
        }
    }

    private List<Map<String, Object>> getLatestDecodedFields() {
        List<SnifferPacket> filtered = getFilteredMonitorPackets()
        if (filtered.isEmpty()) return []
        decodeStructFields(filtered.last().packet)
    }

    private Map<String, Object> handleToggleRecording() {
        if (!packetRecorder) return [success: false, error: "Packet recorder unavailable"]
        String machine = machineContext.fullName()
        if (!recordingActive) {
            boolean started = packetRecorder.startRecording(machine)
            if (!started) return [success: false, error: "Recording already active"]
            recordingActive = true
            return [success: true, recordingActive: true]
        }
        Optional<PacketRecording> stopped = packetRecorder.stopRecording(machine)
        if (stopped.empty) return [success: false, error: "No active recording"]
        recordingActive = false
        lastRecording = stopped.get()
        [success: true, recordingActive: false, recordedPackets: lastRecording.packetCount]
    }

    private Map<String, Object> handleExportRecording() {
        if (!lastRecording) return [success: false, error: "No completed recording to export"]
        String safeMachine = machineContext.fullName().replaceAll("[^A-Za-z0-9._-]", "_")
        buildExportResponse(lastRecording.packets, "recording_${safeMachine}.hex")
    }

    private Map<String, Object> handleExportSelected() {
        if (getSelectedPacketIndices().isEmpty()) return [success: false, error: "No selected packets"]
        List<RecordedPacket> packets = []
        getSelectedPacketIndices().each { idx ->
            if (idx >= 0 && idx < getMonitorPackets().size()) {
                SnifferPacket p = getMonitorPackets()[idx]
                PacketDirection direction = p.packet.packetSource == NetworkPeer.SERVER ? PacketDirection.TO_CLIENT : PacketDirection.TO_SERVER
                packets << RecordedPacket.now(direction, p.packet.opcode, p.packet.toBytes())
            }
        }
        String safeMachine = machineContext.fullName().replaceAll("[^A-Za-z0-9._-]", "_")
        buildExportResponse(packets, "selected_packets_${safeMachine}.hex")
    }

    private Map<String, Object> buildExportResponse(List<RecordedPacket> packets, String filename) {
        String hexDump = packetsToHexDump(packets)
        String binBase64 = Base64.getEncoder().encodeToString(packetsToBinary(packets))
        [success: true, filename: filename, hexDump: hexDump, binaryBase64: binBase64, packetCount: packets.size()]
    }

    private String packetsToHexDump(List<RecordedPacket> packets) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT
        StringBuilder out = new StringBuilder()
        packets.each { p ->
            out.append("# ").append(formatter.format(p.timestamp)).append(" ").append(p.direction)
                .append(" opcode=0x").append(String.format("%04X", p.opcode & 0xFFFF))
                .append(" len=").append(p.dataLength).append("\n")
            byte[] data = p.data
            for (int i = 0; i < data.length; i += 16) {
                int end = Math.min(i + 16, data.length)
                out.append(String.format("%06X  ", i))
                for (int j = i; j < end; j++) out.append(String.format("%02X ", data[j]))
                out.append("\n")
            }
            out.append("\n")
        }
        out.toString()
    }

    private byte[] packetsToBinary(List<RecordedPacket> packets) {
        int total = packets.inject(0) { int acc, RecordedPacket p -> acc + 1 + 4 + p.dataLength }
        ByteBuffer buffer = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        packets.each { p ->
            buffer.put((byte) (p.direction == PacketDirection.TO_SERVER ? 0 : 1))
            buffer.putInt(p.dataLength)
            buffer.put(p.data)
        }
        buffer.array()
    }

    private Map<String, Object> handleInjectPacket(Map<String, Object> input) {
        if (!proxyConnection) return [success: false, error: "Proxy connection unavailable"]
        byte[] bytes = parseHexPayload(str(input.hexPayload))
        if (bytes.length == 0) return [success: false, error: "Empty payload"]
        MutablePacket packet = MutablePacket.wrap(bytes)
        String direction = str(input.direction ?: "C2S")
        if ("S2C".equalsIgnoreCase(direction)) proxyConnection.sendToClient(packet)
        else proxyConnection.sendToServer(packet)
        [success: true, injected: true]
    }

    private byte[] parseHexPayload(String payload) {
        String normalized = (payload ?: "").replaceAll("[^A-Fa-f0-9]", "")
        if (normalized.length() < 2) return new byte[0]
        if ((normalized.length() & 1) == 1) normalized = "0" + normalized
        byte[] out = new byte[normalized.length() / 2]
        for (int i = 0; i < normalized.length(); i += 2) {
            out[i / 2] = (byte) Integer.parseInt(normalized.substring(i, i + 2), 16)
        }
        out
    }

    private static String cacheKey(NetworkPeer source, int opcode) { source.toString() + ":" + opcode }
    private static String str(Object v) { v == null ? "" : String.valueOf(v) }
    private static int num(Object v, int d) {
        if (v instanceof Number) return ((Number) v).intValue()
        if (v instanceof String) {
            try { return Integer.parseInt((String) v) } catch (Exception ignored) {}
        }
        return d
    }

    private static String bytesToHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2]
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF
            out[i * 2] = HEX_ARRAY[v >>> 4]
            out[i * 2 + 1] = HEX_ARRAY[v & 0x0F]
        }
        new String(out)
    }

    private static String toHex(byte[] buf, int ofs, int len) {
        StringBuilder sb = new StringBuilder()
        int end = ofs + len
        for (int i = ofs; i < end; i++) {
            if (i < buf.length) sb.append(String.format("%02x ", buf[i]))
            else sb.append("   ")
        }
        sb.toString()
    }

    private static String toAscii(byte[] buf, int ofs, int len) {
        StringBuilder sb = new StringBuilder()
        int end = ofs + len
        for (int i = ofs; i < end; i++) {
            if (i < buf.length) {
                int c = buf[i] & 0xFF
                sb.append((c >= 32 && c <= 126) ? (char) c : ".")
            } else sb.append(" ")
        }
        sb.toString()
    }

    private int parseOpcode(String opcodeStr) {
        if (!opcodeStr) return 0
        if (opcodeStr.startsWith("0x")) return Integer.parseInt(opcodeStr.substring(2), 16)
        if (opcodeStr ==~ '^[0-9]+$') return Integer.parseInt(opcodeStr)
        Integer.parseInt(opcodeStr, 16)
    }

    private NetworkPeer parseNetworkPeer(String sourceStr) {
        try {
            NetworkPeer.valueOf(sourceStr)
        } catch (Exception ignored) {
            NetworkPeer.CLIENT
        }
    }

    private void saveTracers() {
        mapper.writeValue(tracerFile, getTracers())
    }

    @Override
    void shutdown() {
        try {
            packetStreamSink.tryEmitNext([type: "STREAM_TERMINATED", reason: "PacketSniffer shutdown", timestamp: System.currentTimeMillis()])
        } catch (Exception ignored) {}
        if (packetSubscription) {
            try { packetSubscription.unsubscribe() } catch (Exception ignored) {}
            packetSubscription = null
        }
        setSubscribed(false)
        updateExecutor.shutdown()
        try {
            if (!updateExecutor.awaitTermination(3, TimeUnit.SECONDS)) updateExecutor.shutdownNow()
        } catch (InterruptedException ignored) {
            updateExecutor.shutdownNow()
        }
        super.shutdown()
    }
}

new PacketSnifferPage()
