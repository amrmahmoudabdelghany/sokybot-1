package org.sokybot.proxy.recording;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A complete packet recording session.
 */
public class PacketRecording {

    private final String machineId;
    private final Instant startTime;
    private Instant endTime;
    private final List<RecordedPacket> packets;
    private volatile boolean complete;

    public PacketRecording(String machineId) {
        this.machineId = machineId;
        this.startTime = Instant.now();
        this.packets = Collections.synchronizedList(new ArrayList<>());
        this.complete = false;
    }

    /**
     * Add a packet to the recording.
     */
    public void addPacket(RecordedPacket packet) {
        if (!complete) {
            packets.add(packet);
        }
    }

    /**
     * Mark the recording as complete.
     */
    public void complete() {
        this.endTime = Instant.now();
        this.complete = true;
    }

    public String getMachineId() {
        return machineId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public boolean isComplete() {
        return complete;
    }

    /**
     * Get packet count.
     */
    public int getPacketCount() {
        return packets.size();
    }

    /**
     * Get all packets (unmodifiable).
     */
    public List<RecordedPacket> getPackets() {
        return Collections.unmodifiableList(new ArrayList<>(packets));
    }

    /**
     * Get duration in milliseconds, or -1 if not complete.
     */
    public long getDurationMs() {
        if (endTime == null) {
            return -1;
        }
        return endTime.toEpochMilli() - startTime.toEpochMilli();
    }

    @Override
    public String toString() {
        return String.format("PacketRecording{machineId='%s', packets=%d, complete=%s}",
                machineId, packets.size(), complete);
    }
}
