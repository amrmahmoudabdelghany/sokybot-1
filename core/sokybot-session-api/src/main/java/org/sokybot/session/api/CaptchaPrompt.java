package org.sokybot.session.api;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable description of a captcha or passcode challenge issued by the server.
 * <p>
 * When {@code imageData} is null or empty, the challenge is a simple passcode prompt.
 * When {@code imageData} is present, the challenge includes an image that must be
 * solved (either manually or by an external OCR service).
 */
public final class CaptchaPrompt {

    private final int captchaId;
    private final byte[] imageData;
    private final long issuedAtEpochMs;
    private final String hint;

    /**
     * @param captchaId       server-assigned captcha identifier
     * @param imageData       raw image bytes (null for passcode-only prompts)
     * @param issuedAtEpochMs epoch millis when the challenge was received
     * @param hint            prompt type hint, e.g. {@code "PASSCODE"} or {@code "IMAGE"}
     */
    public CaptchaPrompt(int captchaId, byte[] imageData,
                         long issuedAtEpochMs, String hint) {
        this.captchaId = captchaId;
        this.imageData = imageData == null ? null : imageData.clone();
        this.issuedAtEpochMs = issuedAtEpochMs;
        this.hint = hint;
    }

    public int getCaptchaId() {
        return captchaId;
    }

    /**
     * Returns a defensive copy of the image bytes, or null for passcode-only prompts.
     */
    public byte[] getImageData() {
        return imageData == null ? null : imageData.clone();
    }

    public long getIssuedAtEpochMs() {
        return issuedAtEpochMs;
    }

    /**
     * Gets the prompt type hint (e.g. {@code "PASSCODE"} or {@code "IMAGE"}).
     */
    public String getHint() {
        return hint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CaptchaPrompt)) {
            return false;
        }
        CaptchaPrompt that = (CaptchaPrompt) o;
        return captchaId == that.captchaId
                && issuedAtEpochMs == that.issuedAtEpochMs
                && Arrays.equals(imageData, that.imageData)
                && Objects.equals(hint, that.hint);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(captchaId, issuedAtEpochMs, hint);
        result = 31 * result + Arrays.hashCode(imageData);
        return result;
    }

    @Override
    public String toString() {
        return "CaptchaPrompt{captchaId=" + captchaId
                + ", imageSize=" + (imageData == null ? 0 : imageData.length)
                + ", hint='" + hint + "'}";
    }
}
