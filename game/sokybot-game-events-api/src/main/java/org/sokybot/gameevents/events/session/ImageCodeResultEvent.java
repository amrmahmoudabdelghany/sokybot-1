package org.sokybot.gameevents.events.session;

import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Gateway image-code verification result (vSRO-style {@code 0xA323} after client {@code 0x6323}).
 */
public class ImageCodeResultEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final byte resultCode;

    public ImageCodeResultEvent(String machineFullName, boolean success, byte resultCode) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.resultCode = resultCode;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public byte getResultCode() {
        return resultCode;
    }
}
