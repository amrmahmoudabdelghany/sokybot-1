import { useRef, useCallback } from 'react';

/**
 * Per-machine stream thrashing protection.
 *
 * Monitors transition rate and activates a circuit breaker if events
 * exceed a threshold (default: 20 transitions in 2 seconds).
 *
 * When tripped, the breaker throttles or severs the noisy subscription
 * and exposes a recovery action to re-subscribe.
 */

const DEFAULT_THRESHOLD = 20;
const DEFAULT_WINDOW_MS = 2000;

interface UseStreamThrottleGuardOptions {
    /** Maximum transitions allowed in the window. */
    threshold?: number;
    /** Window duration in ms. */
    windowMs?: number;
}

interface StreamThrottleGuardState {
    /** Whether the circuit breaker is currently tripped. */
    isTripped: boolean;
    /** Record a transition. Returns true if breaker is now tripped. */
    recordTransition: () => boolean;
    /** Reset the breaker (user-initiated recovery). */
    reset: () => void;
}

export function useStreamThrottleGuard(
    options?: UseStreamThrottleGuardOptions,
): StreamThrottleGuardState {
    const threshold = options?.threshold ?? DEFAULT_THRESHOLD;
    const windowMs = options?.windowMs ?? DEFAULT_WINDOW_MS;

    const timestampsRef = useRef<number[]>([]);
    const trippedRef = useRef(false);

    const recordTransition = useCallback((): boolean => {
        if (trippedRef.current) return true;

        const now = Date.now();
        const cutoff = now - windowMs;
        const timestamps = timestampsRef.current.filter((t) => t > cutoff);
        timestamps.push(now);
        timestampsRef.current = timestamps;

        if (timestamps.length > threshold) {
            trippedRef.current = true;
            return true;
        }
        return false;
    }, [threshold, windowMs]);

    const reset = useCallback(() => {
        trippedRef.current = false;
        timestampsRef.current = [];
    }, []);

    return {
        get isTripped() {
            return trippedRef.current;
        },
        recordTransition,
        reset,
    };
}
