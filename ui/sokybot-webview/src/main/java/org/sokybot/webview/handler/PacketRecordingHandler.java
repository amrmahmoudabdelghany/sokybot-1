package org.sokybot.webview.handler;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.proxy.recording.IPacketRecorder;
import org.sokybot.proxy.recording.PacketRecording;
import org.sokybot.proxy.recording.RecordedPacket;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Mono;

/**
 * RSocket handler for packet recording operations.
 * 
 * Methods:
 * - recording.start: Start recording for a machine
 * - recording.stop: Stop recording and return summary
 * - recording.status: Get recording status for a machine
 * - recording.list: List active recordings
 */
@Component(service = IRSocketHandler.class, property = {
        IRSocketHandler.METHOD_PROPERTY + "=recording.start",
        IRSocketHandler.METHOD_PROPERTY + "=recording.stop",
        IRSocketHandler.METHOD_PROPERTY + "=recording.status",
        IRSocketHandler.METHOD_PROPERTY + "=recording.list",
        IRSocketHandler.METHOD_PROPERTY + "=recording.export"
})
public class PacketRecordingHandler implements IRSocketHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private volatile IPacketRecorder packetRecorder;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setPacketRecorder(IPacketRecorder recorder) {
        this.packetRecorder = recorder;
    }

    protected void unsetPacketRecorder(IPacketRecorder recorder) {
        this.packetRecorder = null;
    }

    @Override
    public String[] getMethods() {
        return new String[] { "recording.start", "recording.stop", "recording.status", "recording.list", "recording.export" };
    }

    @Override
    public String getDescription() {
        return "Packet recording operations for debugging";
    }

    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        String method = request.getMethod();

        switch (method) {
            case "recording.start":
                return handleStart(request);
            case "recording.stop":
                return handleStop(request);
            case "recording.status":
                return handleStatus(request);
            case "recording.list":
                return handleList(request);
            case "recording.export":
                return handleExport(request);
            default:
                return Mono.just(RSocketResponse.methodNotFound(method));
        }
    }

    private Mono<RSocketResponse> handleStart(RSocketRequest request) {
        if (packetRecorder == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Packet recorder not available"));
        }

        String machineId = request.getString("machineId");
        if (machineId == null || machineId.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: machineId"));
        }

        boolean started = packetRecorder.startRecording(machineId);

        Map<String, Object> response = new HashMap<>();
        response.put("machineId", machineId);
        response.put("started", started);
        if (!started) {
            response.put("message", "Already recording for this machine");
        }

        return Mono.just(RSocketResponse.success(response));
    }

    private Mono<RSocketResponse> handleStop(RSocketRequest request) {
        if (packetRecorder == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Packet recorder not available"));
        }

        String machineId = request.getString("machineId");
        if (machineId == null || machineId.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: machineId"));
        }

        return packetRecorder.stopRecording(machineId)
                .map(recording -> {
                    Map<String, Object> response = recordingToSummary(recording);
                    response.put("stopped", true);
                    return RSocketResponse.success(response);
                })
                .map(Mono::just)
                .orElse(Mono.just(RSocketResponse.error(
                        RSocketResponse.ErrorCode.NOT_FOUND,
                        "No active recording for machine: " + machineId)));
    }

    private Mono<RSocketResponse> handleStatus(RSocketRequest request) {
        if (packetRecorder == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Packet recorder not available"));
        }

        String machineId = request.getString("machineId");
        if (machineId == null || machineId.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: machineId"));
        }

        boolean isRecording = packetRecorder.isRecording(machineId);

        Map<String, Object> response = new HashMap<>();
        response.put("machineId", machineId);
        response.put("recording", isRecording);

        if (isRecording) {
            packetRecorder.getActiveRecording(machineId).ifPresent(recording -> {
                response.put("packetCount", recording.getPacketCount());
                response.put("startTime", TIMESTAMP_FORMATTER.format(recording.getStartTime()));
            });
        }

        return Mono.just(RSocketResponse.success(response));
    }

    private Mono<RSocketResponse> handleList(RSocketRequest request) {
        if (packetRecorder == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Packet recorder not available"));
        }

        List<String> activeRecordings = packetRecorder.getActiveRecordings();

        List<Map<String, Object>> recordings = new ArrayList<>();
        for (String machineId : activeRecordings) {
            packetRecorder.getActiveRecording(machineId).ifPresent(recording -> {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("machineId", machineId);
                info.put("packetCount", recording.getPacketCount());
                info.put("startTime", TIMESTAMP_FORMATTER.format(recording.getStartTime()));
                recordings.add(info);
            });
        }

        Map<String, Object> response = new HashMap<>();
        response.put("recordings", recordings);
        response.put("count", recordings.size());

        return Mono.just(RSocketResponse.success(response));
    }

    private Map<String, Object> recordingToSummary(PacketRecording recording) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("machineId", recording.getMachineId());
        summary.put("packetCount", recording.getPacketCount());
        summary.put("startTime", TIMESTAMP_FORMATTER.format(recording.getStartTime()));
        if (recording.getEndTime() != null) {
            summary.put("endTime", TIMESTAMP_FORMATTER.format(recording.getEndTime()));
            summary.put("durationMs", recording.getDurationMs());
        }
        return summary;
    }

    private Mono<RSocketResponse> handleExport(RSocketRequest request) {
        if (packetRecorder == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Packet recorder not available"));
        }

        String machineId = request.getString("machineId");
        if (machineId == null || machineId.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: machineId"));
        }

        PacketRecording recording = packetRecorder.getActiveRecording(machineId).orElse(null);
        if (recording == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.NOT_FOUND,
                    "No active recording for machine: " + machineId));
        }

        List<RecordedPacket> packets = recording.getPackets();
        String hexDump = toHexDump(packets);
        String binaryBase64 = Base64.getEncoder().encodeToString(toBinary(packets));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("machineId", machineId);
        response.put("packetCount", packets.size());
        response.put("hexDump", hexDump);
        response.put("binaryBase64", binaryBase64);
        return Mono.just(RSocketResponse.success(response));
    }

    private String toHexDump(List<RecordedPacket> packets) {
        StringBuilder out = new StringBuilder();
        for (RecordedPacket packet : packets) {
            out.append("# ").append(TIMESTAMP_FORMATTER.format(packet.getTimestamp()))
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
                out.append('\n');
            }
            out.append('\n');
        }
        return out.toString();
    }

    private byte[] toBinary(List<RecordedPacket> packets) {
        int total = 0;
        for (RecordedPacket packet : packets) {
            total += 1 + 4 + packet.getDataLength();
        }
        ByteBuffer buffer = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        for (RecordedPacket packet : packets) {
            buffer.put((byte) (packet.getDirection().name().contains("SERVER") ? 0 : 1));
            buffer.putInt(packet.getDataLength());
            buffer.put(packet.getData());
        }
        return buffer.array();
    }
}
