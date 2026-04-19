package org.sokybot.gameevents.events.session;

import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event emitted after a captcha/passcode challenge has been resolved
 * (either accepted or timed out).
 */
public class CaptchaResolvedEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;
    private final int captchaId;
    private final boolean accepted;
    private final String solverId;

    /**
     * @param fullName  machine full name (group.machine)
     * @param captchaId the captcha prompt ID that was resolved
     * @param accepted  true if the server accepted the answer
     * @param solverId  identifier of the solver that produced the answer
     *                  (e.g. "blind-passcode", "manual-alert")
     */
    public CaptchaResolvedEvent(String fullName, int captchaId,
                                boolean accepted, String solverId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.captchaId = captchaId;
        this.accepted = accepted;
        this.solverId = solverId;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the captcha prompt ID that was resolved.
     */
    public int getCaptchaId() {
        return captchaId;
    }

    /**
     * Returns true if the server accepted the submitted answer.
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Gets the identifier of the solver that produced the answer.
     */
    public String getSolverId() {
        return solverId;
    }

    @Override
    public String toString() {
        return "CaptchaResolvedEvent{fullName='" + fullName
                + "', captchaId=" + captchaId
                + ", accepted=" + accepted
                + ", solverId='" + solverId + "'}";
    }
}
