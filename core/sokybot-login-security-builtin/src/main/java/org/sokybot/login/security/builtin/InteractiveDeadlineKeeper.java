package org.sokybot.login.security.builtin;

import java.util.Map;

import org.sokybot.engine.api.workflow.IWorkflowContext;

final class InteractiveDeadlineKeeper {

    static final String KEY_INTERACTIVE_WAIT_UNTIL_MS = "loginInteractiveWaitUntilMs";
    static final String KEY_USER_RESUME_REQUIRED = "loginUserResumeRequired";

    long ensure(IWorkflowContext context, long timeoutMs) {
        long now = System.currentTimeMillis();
        long existing = read(context);
        if (existing > now) {
            return existing;
        }
        long deadline = now + Math.max(1000L, timeoutMs);
        data(context).put(KEY_INTERACTIVE_WAIT_UNTIL_MS, Long.valueOf(deadline));
        return deadline;
    }

    long read(IWorkflowContext context) {
        Object value = data(context).get(KEY_INTERACTIVE_WAIT_UNTIL_MS);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    boolean isTimedOut(IWorkflowContext context) {
        long deadline = read(context);
        return deadline > 0L && System.currentTimeMillis() >= deadline;
    }

    void clear(IWorkflowContext context) {
        data(context).remove(KEY_INTERACTIVE_WAIT_UNTIL_MS);
    }

    void markUserResumeRequired(IWorkflowContext context) {
        data(context).put(KEY_USER_RESUME_REQUIRED, Boolean.TRUE);
    }

    private static Map<String, Object> data(IWorkflowContext context) {
        return context.getPersistentData();
    }
}
