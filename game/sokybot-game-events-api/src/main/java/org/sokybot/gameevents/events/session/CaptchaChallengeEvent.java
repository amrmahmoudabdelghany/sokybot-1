package org.sokybot.gameevents.events.session;

import org.sokybot.gameevents.events.core.IGameEvent;

public class CaptchaChallengeEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;
    private final int captchaId;
    private final byte[] imageData;

    public CaptchaChallengeEvent(String machineFullName, int captchaId, byte[] imageData) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.captchaId = captchaId;
        this.imageData = imageData == null ? null : imageData.clone();
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public int getCaptchaId() {
        return captchaId;
    }

    public byte[] getImageData() {
        return imageData == null ? null : imageData.clone();
    }
}
