package org.sokybot.proxy.recording;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of packet recorder.
 * 
 * Stores active recordings in memory.
 */
@Component(service = IPacketRecorder.class)
public class PacketRecorderImpl implements IPacketRecorder {

    private static final Logger log = LoggerFactory.getLogger(PacketRecorderImpl.class);

    /**
     * Active recordings keyed by machine ID.
     */
    private final Map<String, PacketRecording> activeRecordings = new ConcurrentHashMap<>();

    /**
     * Completed recordings (kept for retrieval).
     */
    private final Map<String, PacketRecording> completedRecordings = new ConcurrentHashMap<>();

    /**
     * Maximum number of completed recordings to keep.
     */
    private static final int MAX_COMPLETED_RECORDINGS = 10;

    @Activate
    protected void activate() {
        log.info("Packet Recorder activated");
    }

    @Deactivate
    protected void deactivate() {
        // Complete any active recordings
        for (PacketRecording recording : activeRecordings.values()) {
            recording.complete();
            log.info("Force-completed recording for {}", recording.getMachineId());
        }
        activeRecordings.clear();
        completedRecordings.clear();
    }

    @Override
    public boolean startRecording(String machineId) {
        if (machineId == null || machineId.isEmpty()) {
            return false;
        }

        if (activeRecordings.containsKey(machineId)) {
            log.warn("Already recording for machine: {}", machineId);
            return false;
        }

        PacketRecording recording = new PacketRecording(machineId);
        activeRecordings.put(machineId, recording);
        log.info("Started recording for machine: {}", machineId);
        return true;
    }

    @Override
    public Optional<PacketRecording> stopRecording(String machineId) {
        PacketRecording recording = activeRecordings.remove(machineId);

        if (recording == null) {
            return Optional.empty();
        }

        recording.complete();

        // Store completed recording
        completedRecordings.put(machineId, recording);
        trimCompletedRecordings();

        log.info("Stopped recording for machine: {}, packets: {}, duration: {}ms",
                machineId, recording.getPacketCount(), recording.getDurationMs());

        return Optional.of(recording);
    }

    @Override
    public boolean isRecording(String machineId) {
        return activeRecordings.containsKey(machineId);
    }

    @Override
    public List<String> getActiveRecordings() {
        return new ArrayList<>(activeRecordings.keySet());
    }

    @Override
    public Optional<PacketRecording> getActiveRecording(String machineId) {
        return Optional.ofNullable(activeRecordings.get(machineId));
    }

    @Override
    public void recordPacket(String machineId, RecordedPacket packet) {
        PacketRecording recording = activeRecordings.get(machineId);
        if (recording != null) {
            recording.addPacket(packet);
        }
    }

    /**
     * Get a completed recording.
     */
    public Optional<PacketRecording> getCompletedRecording(String machineId) {
        return Optional.ofNullable(completedRecordings.get(machineId));
    }

    /**
     * Get list of completed recording machine IDs.
     */
    public List<String> getCompletedRecordingIds() {
        return new ArrayList<>(completedRecordings.keySet());
    }

    /**
     * Trim completed recordings to max size.
     */
    private void trimCompletedRecordings() {
        while (completedRecordings.size() > MAX_COMPLETED_RECORDINGS) {
            // Remove oldest (this is approximate, but good enough)
            String oldest = Collections.min(completedRecordings.keySet());
            completedRecordings.remove(oldest);
        }
    }
}
