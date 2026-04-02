import { useState, useEffect, useRef, useCallback } from 'react';
import type { Severity } from './loginPhaseMapping';

/**
 * Hook providing asymmetric debounce for display-only phase rendering.
 *
 * Behaviour:
 *   - Error/failed severities: bypass debounce (immediate display)
 *   - Other severities (neutral/info/success): debounce by DEBOUNCE_MS
 *   - Sticky error hold: errors stay visible for STICKY_ERROR_MS before
 *     being replaced by non-error state
 *
 * All timers are cleaned up on unmount and when machineId changes.
 */

const DEBOUNCE_MS = 400;
const STICKY_ERROR_MS = 1500;

interface StablePhaseState<T> {
    /** The debounced/stabilized value for rendering. */
    stable: T;
}

/**
 * Asymmetric debounce for phase display values.
 *
 * @param current - The live value from the machine context
 * @param severity - Current severity (errors bypass debounce)
 * @param machineId - Machine context scope (resets timers on change)
 */
export function useStablePhase<T>(
    current: T,
    severity: Severity,
    machineId: string,
): StablePhaseState<T> {
    const [stable, setStable] = useState(current);
    const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
    const stickyUntilRef = useRef<number>(0);

    const clearTimer = useCallback(() => {
        if (debounceTimer.current) {
            clearTimeout(debounceTimer.current);
            debounceTimer.current = null;
        }
    }, []);

    // Reset everything when machine context changes
    useEffect(() => {
        clearTimer();
        stickyUntilRef.current = 0;
        setStable(current);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [machineId]);

    useEffect(() => {
        const isError = severity === 'error';
        const now = Date.now();

        // Error/failed → immediate display
        if (isError) {
            clearTimer();
            setStable(current);
            stickyUntilRef.current = now + STICKY_ERROR_MS;
            return;
        }

        // Non-error replacing an error → respect sticky hold
        const stickyRemaining = stickyUntilRef.current - now;
        if (stickyRemaining > 0) {
            clearTimer();
            debounceTimer.current = setTimeout(() => {
                setStable(current);
                stickyUntilRef.current = 0;
            }, stickyRemaining);
            return;
        }

        // Normal debounce for non-error transitions
        clearTimer();
        debounceTimer.current = setTimeout(() => {
            setStable(current);
        }, DEBOUNCE_MS);

        return clearTimer;
    }, [current, severity, clearTimer]);

    // Cleanup on unmount
    useEffect(() => {
        return clearTimer;
    }, [clearTimer]);

    return { stable };
}
