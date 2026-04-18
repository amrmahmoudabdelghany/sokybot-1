package org.sokybot.login.settings.builtin;

final class LoginTimeoutNormalizer {

    private LoginTimeoutNormalizer() {
    }

    static long normalize(long value, long fallback, long min, long max) {
        long raw = value > 0L ? value : fallback;
        return Math.max(min, Math.min(max, raw));
    }

    static int normalize(int value, int fallback, int min, int max) {
        int raw = value > 0 ? value : fallback;
        return Math.max(min, Math.min(max, raw));
    }

    static Normalized normalized(
            long agentWaitTimeoutMs,
            long loginResponseTimeoutMs,
            int agentRequestMaxRetries,
            long agentRequestRetryBackoffMs
    ) {
        long adjustedAgentWait = agentWaitTimeoutMs;
        if (adjustedAgentWait >= loginResponseTimeoutMs) {
            adjustedAgentWait = Math.max(1000L, loginResponseTimeoutMs - 1000L);
        }
        int retries = Math.max(0, agentRequestMaxRetries);
        long backoff = Math.max(500L, agentRequestRetryBackoffMs);
        long totalBudget = adjustedAgentWait * (retries + 1L) + (backoff * retries);
        if (totalBudget > loginResponseTimeoutMs) {
            long divisor = Math.max(1L, adjustedAgentWait + backoff);
            long headroom = loginResponseTimeoutMs - adjustedAgentWait;
            long computed = headroom > 0L ? headroom / divisor : 0L;
            retries = (int) Math.max(0L, computed);
        }
        return new Normalized(adjustedAgentWait, retries);
    }

    static final class Normalized {
        private final long agentWaitTimeoutMs;
        private final int agentRequestMaxRetries;

        private Normalized(long agentWaitTimeoutMs, int agentRequestMaxRetries) {
            this.agentWaitTimeoutMs = agentWaitTimeoutMs;
            this.agentRequestMaxRetries = agentRequestMaxRetries;
        }

        long getAgentWaitTimeoutMs() {
            return agentWaitTimeoutMs;
        }

        int getAgentRequestMaxRetries() {
            return agentRequestMaxRetries;
        }
    }
}
