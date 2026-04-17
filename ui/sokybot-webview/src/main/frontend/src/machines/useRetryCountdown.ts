import { useState, useEffect, useCallback, useRef } from 'react';

/**
 * Hook managing a live retry countdown timer.
 *
 * Timing source precedence:
 *   1. retryAt (absolute UTC ms)  – preferred
 *   2. retryDelayMs + serverTimestamp – computed absolute
 *   3. retryDelayMs + localReceiptTime – optimistic fallback
 *
 * Features:
 *   - Live countdown updated every 200ms
 *   - Disable "Retry now" in final 500ms (boundary dedupe)
 *   - Fail-safe unlock: if countdown reaches 0 and no phase transition
 *     in STALL_TIMEOUT_MS, re-enable retry and show stall warning
 *   - Pauses when `isOffline` is true
 *   - Hidden when `fatal` is true (fatal failures never auto-retry)
 */

interface UseRetryCountdownOptions {
    /** Phase-derived: is the current phase RETRY_DELAY? */
    isRetryPhase: boolean;
    /** Absolute UTC ms when retry will fire (from backend). */
    retryAt: number | null;
    /** Retry delay in ms (from backend). */
    retryDelayMs: number | null;
    /** Server timestamp in ms when retry was scheduled (from backend). */
    serverTimestamp: number | null;
    /** Whether the failure is fatal (non-retryable). */
    fatal: boolean;
    /** Whether the host reports offline. */
    isOffline: boolean;
    /** Callback when user triggers manual retry. */
    onRetryNow: () => void;
}

export interface RetryCountdownState {
    /** Seconds remaining (0 when expired). */
    remainingSeconds: number;
    /** Whether the Retry Now button should be enabled. */
    canRetryNow: boolean;
    /** Whether the countdown is active/visible. */
    isActive: boolean;
    /** Whether a stall has been detected (auto-retry may have failed). */
    isStalled: boolean;
    /** Countdown denominator derived from initial retry delay input. */
    totalSeconds: number;
    /** Trigger a manual retry (with dedupe guard). */
    triggerRetry: () => void;
}

const BOUNDARY_GUARD_MS = 500;
const STALL_TIMEOUT_MS = 3000;
const TICK_INTERVAL_MS = 200;

export function useRetryCountdown({
    isRetryPhase,
    retryAt,
    retryDelayMs,
    serverTimestamp,
    fatal,
    isOffline,
    onRetryNow,
}: UseRetryCountdownOptions): RetryCountdownState {
    const [remainingMs, setRemainingMs] = useState(0);
    const [totalMs, setTotalMs] = useState(1000);
    const [isStalled, setIsStalled] = useState(false);
    const [retryInProgress, setRetryInProgress] = useState(false);
    const [localScheduleTimestamp, setLocalScheduleTimestamp] = useState(0);
    const stallTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    // Compute target time from available sources
    const targetMs = (() => {
        if (!isRetryPhase || fatal) return null;
        if (retryAt != null && retryAt > 0) return retryAt;
        if (retryDelayMs != null && retryDelayMs > 0) {
            const base = (serverTimestamp != null && serverTimestamp > 0)
                ? serverTimestamp
                : localScheduleTimestamp;
            return base + retryDelayMs;
        }
        return null;
    })();

    // Reset stall state when phase changes
    useEffect(() => {
        const resetId = setTimeout(() => {
            setLocalScheduleTimestamp(Date.now());
            setIsStalled(false);
            setRetryInProgress(false);
        }, 0);
        const initial = retryDelayMs != null && retryDelayMs > 0
            ? retryDelayMs
            : 1000;
        const totalId = setTimeout(() => setTotalMs(Math.max(1000, initial)), 0);
        if (stallTimerRef.current) {
            clearTimeout(stallTimerRef.current);
            stallTimerRef.current = null;
        }
        return () => {
            clearTimeout(resetId);
            clearTimeout(totalId);
        };
    }, [isRetryPhase, retryAt, retryDelayMs]);

    // Countdown tick
    useEffect(() => {
        if (targetMs == null || isOffline) {
            const id = setTimeout(() => setRemainingMs(0), 0);
            return () => clearTimeout(id);
        }

        const tick = () => {
            const now = Date.now();
            const rem = Math.max(0, targetMs - now);
            setRemainingMs(rem);

            // Start stall timer when countdown reaches 0
            if (rem <= 0 && !stallTimerRef.current) {
                stallTimerRef.current = setTimeout(() => {
                    setIsStalled(true);
                    setRetryInProgress(false);
                }, STALL_TIMEOUT_MS);
            }
        };

        tick();
        const id = setInterval(tick, TICK_INTERVAL_MS);
        return () => {
            clearInterval(id);
            if (stallTimerRef.current) {
                clearTimeout(stallTimerRef.current);
                stallTimerRef.current = null;
            }
        };
    }, [targetMs, isOffline]);

    const canRetryNow = isRetryPhase
        && !fatal
        && !isOffline
        && !retryInProgress
        && (remainingMs > BOUNDARY_GUARD_MS || isStalled);

    const triggerRetry = useCallback(() => {
        if (!canRetryNow || retryInProgress) return;
        setRetryInProgress(true);
        setIsStalled(false);
        if (stallTimerRef.current) {
            clearTimeout(stallTimerRef.current);
            stallTimerRef.current = null;
        }
        onRetryNow();
    }, [canRetryNow, onRetryNow, retryInProgress]);

    const isActive = isRetryPhase && !fatal && targetMs != null;
    const remainingSeconds = Math.ceil(remainingMs / 1000);
    const totalSeconds = Math.max(1, Math.ceil(totalMs / 1000));

    return {
        remainingSeconds,
        canRetryNow,
        isActive,
        isStalled,
        totalSeconds,
        triggerRetry,
    };
}
