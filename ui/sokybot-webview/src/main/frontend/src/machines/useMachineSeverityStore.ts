import { create } from 'zustand';
import type { Severity } from './loginPhaseMapping';

/**
 * Per-machine severity store for background health indicators.
 *
 * Updates only on severity-change deltas to minimize re-renders.
 * Used by Layout.tsx to show badge/dot severity on machine list items.
 */

interface MachineSeverityState {
    /** Map of machineId → current severity. */
    severityByMachine: Record<string, Severity>;
    /** Update severity for a machine (no-op if unchanged). */
    setSeverity: (machineId: string, severity: Severity) => void;
    /** Clear severity entry when machine is removed. */
    clearMachine: (machineId: string) => void;
}

export const useMachineSeverityStore = create<MachineSeverityState>((set, get) => ({
    severityByMachine: {},
    setSeverity: (machineId, severity) => {
        const current = get().severityByMachine[machineId];
        if (current === severity) return; // No-op on same value
        set((state) => ({
            severityByMachine: { ...state.severityByMachine, [machineId]: severity },
        }));
    },
    clearMachine: (machineId) => {
        set((state) => {
            const next = { ...state.severityByMachine };
            delete next[machineId];
            return { severityByMachine: next };
        });
    },
}));
