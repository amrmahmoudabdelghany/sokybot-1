package org.sokybot.session.api;

/**
 * SPI for submitting captcha answers from the UI layer.
 * <p>
 * The webview controller (or any other UI component) injects this service
 * to relay operator-provided captcha answers to the blocking solver.
 * <p>
 * Implementation lives in {@code sokybot-session-solvers-builtin}.
 */
public interface ICaptchaInbox {

    /**
     * Submit a captcha answer from the UI.
     *
     * @param machineId the machine this captcha belongs to
     * @param captchaId the captcha prompt ID
     * @param answer    the user-provided answer string
     */
    void submit(String machineId, int captchaId, String answer);
}
