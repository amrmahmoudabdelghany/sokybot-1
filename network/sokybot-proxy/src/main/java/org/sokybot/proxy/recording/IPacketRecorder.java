package org.sokybot.proxy.recording;

import java.util.List;
import java.util.Optional;

/**
 * Service for recording packet sessions.
 * 
 * Records packets flowing through the proxy for debugging and analysis.
 */
public interface IPacketRecorder {

    /**
     * Start recording packets for a machine.
     * 
     * @param machineId the machine to record
     * @return true if recording started, false if already recording
     */
    boolean startRecording(String machineId);

    /**
     * Stop recording and return the completed recording.
     * 
     * @param machineId the machine to stop recording
     * @return the completed recording, or empty if not recording
     */
    Optional<PacketRecording> stopRecording(String machineId);

    /**
     * Check if a machine is currently being recorded.
     * 
     * @param machineId the machine ID
     * @return true if recording is active
     */
    boolean isRecording(String machineId);

    /**
     * Get list of machines currently being recorded.
     * 
     * @return list of machine IDs
     */
    List<String> getActiveRecordings();

    /**
     * Get a recording in progress (for live monitoring).
     * 
     * @param machineId the machine ID
     * @return the active recording, or empty if not recording
     */
    Optional<PacketRecording> getActiveRecording(String machineId);

    /**
     * Record a packet observation.
     * Called by the packet observer when a packet is received.
     * 
     * @param machineId the machine ID
     * @param packet    the recorded packet
     */
    void recordPacket(String machineId, RecordedPacket packet);
}
