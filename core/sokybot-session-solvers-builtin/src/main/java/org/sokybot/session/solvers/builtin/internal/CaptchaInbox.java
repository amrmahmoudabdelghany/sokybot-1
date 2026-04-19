package org.sokybot.session.solvers.builtin.internal;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.session.api.ICaptchaInbox;

/**
 * Bridge between the UI layer and blocking captcha solvers.
 * <p>
 * The UI calls {@link #submit(String, int, String)} to relay the operator's
 * answer. The solver calls {@link #awaitAnswer(String, int, long)} to block
 * until the answer arrives or the deadline expires.
 * <p>
 * Keyed by {@code "machineId:captchaId"} so multiple machines can have
 * independent captcha sessions.
 */
@Component(service = { ICaptchaInbox.class, CaptchaInbox.class })
public final class CaptchaInbox implements ICaptchaInbox {

    private static final Logger log = LoggerFactory.getLogger(CaptchaInbox.class);

    private final ConcurrentMap<String, BlockingQueue<String>> pending = new ConcurrentHashMap<>();

    @Override
    public void submit(String machineId, int captchaId, String answer) {
        String key = buildKey(machineId, captchaId);
        BlockingQueue<String> queue = pending.get(key);
        if (queue != null) {
            boolean offered = queue.offer(answer);
            log.debug("Captcha answer submitted for key={}, accepted={}", key, offered);
        } else {
            log.warn("No pending captcha for key={}. Answer discarded.", key);
        }
    }

    /**
     * Blocks until the operator submits an answer or the deadline expires.
     * <p>
     * Package-private: called by {@link ManualUserAlertSolver}.
     *
     * @param machineId       the machine identifier
     * @param captchaId       the captcha prompt ID
     * @param deadlineEpochMs hard deadline (epoch millis)
     * @return the answer or empty on timeout / interruption
     */
    Optional<String> awaitAnswer(String machineId, int captchaId, long deadlineEpochMs) {
        String key = buildKey(machineId, captchaId);
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(1);
        pending.put(key, queue);
        try {
            long waitMs = Math.max(0, deadlineEpochMs - System.currentTimeMillis());
            log.debug("Awaiting captcha answer for key={}, timeoutMs={}", key, waitMs);
            String answer = queue.poll(waitMs, TimeUnit.MILLISECONDS);
            return Optional.ofNullable(answer);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Captcha await interrupted for key={}", key);
            return Optional.empty();
        } finally {
            pending.remove(key);
        }
    }

    private static String buildKey(String machineId, int captchaId) {
        return machineId + ":" + captchaId;
    }
}
