import type { MutableRefObject } from 'react';
import { useEffect, useRef, useState } from 'react';
import { rsocketService } from '../RSocketClient';
import { streamEventToMachineEvent } from '../machines/machineOnboarding.machine';
import type { OnboardingMachineEvent } from '../machines/machineOnboarding.machine';

type Params = {
    machineId: string;
    send: (event: OnboardingMachineEvent) => void;
    refreshMachineStatusSnapshot: (id: string, attempt?: number) => Promise<void>;
    isHighPriorityPhase: (phase?: string) => boolean;
    throttleGuard: { recordTransition: () => boolean };
    abortInFlightRef: MutableRefObject<Record<string, boolean>>;
    loginPhaseRef: MutableRefObject<string>;
    onMachineRemoved?: () => void;
};

const AGENT_WAIT_LOGIN_PHASES = new Set([
    'WAITING_FOR_AGENTS',
    'WAITING_FOR_AGENTS_TIMEOUT',
    'MISSING_AGENT_SERVER',
    'AGENTS_RECEIVED',
    'REDIRECTING',
    'SERVER_INSPECTION',
]);

export function useMachineStatusStream({
    machineId,
    send,
    refreshMachineStatusSnapshot,
    isHighPriorityPhase,
    throttleGuard,
    abortInFlightRef,
    loginPhaseRef,
}: Params) {
    const streamSessionIdRef = useRef<number>(0);
    const terminalPhaseGateUntilRef = useRef<number>(0);
    const machineRemovedRef = useRef(false);
    const lastStreamEventAtRef = useRef<number>(0);
    const streamReconnectAttemptRef = useRef(0);
    const streamResubscribePendingRef = useRef(false);
    const [streamEpoch, setStreamEpoch] = useState(0);
    const lastHeartbeatSnapshotAtRef = useRef<number>(0);

    useEffect(() => {
        const currentSessionId = ++streamSessionIdRef.current;
        machineRemovedRef.current = false;
        lastStreamEventAtRef.current = Date.now();
        streamReconnectAttemptRef.current = 0;
        streamResubscribePendingRef.current = false;

        void refreshMachineStatusSnapshot(machineId);
        const sub = rsocketService.subscribeToMachineStatus(
            machineId,
            (statusEvent) => {
                if (streamSessionIdRef.current !== currentSessionId) return;
                if (!statusEvent || statusEvent.machineId !== machineId) return;
                if (machineRemovedRef.current) return;

                if (statusEvent.type === 'heartbeat') {
                    lastStreamEventAtRef.current = Date.now();
                    streamReconnectAttemptRef.current = 0;
                    const phase = loginPhaseRef.current;
                    if (phase && AGENT_WAIT_LOGIN_PHASES.has(phase)) {
                        const now = Date.now();
                        if (now - lastHeartbeatSnapshotAtRef.current >= 4000) {
                            lastHeartbeatSnapshotAtRef.current = now;
                            void refreshMachineStatusSnapshot(machineId);
                        }
                    }
                    return;
                }

                if (statusEvent.type === 'MACHINE_REMOVED') {
                    machineRemovedRef.current = true;
                    lastStreamEventAtRef.current = Date.now();
                    send(streamEventToMachineEvent({
                        ...statusEvent,
                        machineId,
                        loginPhase: 'DISCONNECTED',
                        connected: false,
                        authenticated: false,
                    }));
                    return;
                }

                lastStreamEventAtRef.current = Date.now();
                streamReconnectAttemptRef.current = 0;

                if (
                    abortInFlightRef.current[statusEvent.machineId]
                    && statusEvent.loginPhase !== 'FAILED'
                    && statusEvent.loginPhase !== 'DISCONNECTED'
                ) {
                    return;
                }

                if (
                    terminalPhaseGateUntilRef.current > Date.now()
                    && !isHighPriorityPhase(statusEvent.loginPhase)
                ) {
                    void refreshMachineStatusSnapshot(machineId);
                    return;
                }

                if (!isHighPriorityPhase(statusEvent.loginPhase) && throttleGuard.recordTransition()) {
                    void refreshMachineStatusSnapshot(machineId);
                    return;
                }
                if (isHighPriorityPhase(statusEvent.loginPhase)) {
                    terminalPhaseGateUntilRef.current = Date.now() + 1200;
                }
                send(streamEventToMachineEvent(statusEvent));
                void refreshMachineStatusSnapshot(machineId);
            },
            (err) => console.error('Layout machine status stream error', err)
        );

        const stalenessId = window.setInterval(() => {
            if (machineRemovedRef.current || streamResubscribePendingRef.current) {
                return;
            }
            if (Date.now() - lastStreamEventAtRef.current <= 15_000) {
                return;
            }
            streamResubscribePendingRef.current = true;
            const nextAttempt = streamReconnectAttemptRef.current + 1;
            streamReconnectAttemptRef.current = Math.min(nextAttempt, 16);
            const base = Math.min(30_000, 500 * Math.pow(2, Math.max(0, nextAttempt - 1)));
            const jitter = Math.random() * 2000;
            window.setTimeout(() => {
                streamResubscribePendingRef.current = false;
                if (machineRemovedRef.current) {
                    return;
                }
                setStreamEpoch((e) => e + 1);
            }, base + jitter);
        }, 5000);

        const softSyncId = window.setInterval(() => {
            if (machineRemovedRef.current) {
                return;
            }
            if (Date.now() - lastStreamEventAtRef.current > 15_000) {
                return;
            }
            void refreshMachineStatusSnapshot(machineId);
        }, 60_000);

        return () => {
            window.clearInterval(stalenessId);
            window.clearInterval(softSyncId);
            if (sub && typeof sub.unsubscribe === 'function') {
                sub.unsubscribe();
            }
        };
    }, [
        abortInFlightRef,
        isHighPriorityPhase,
        loginPhaseRef,
        machineId,
        refreshMachineStatusSnapshot,
        send,
        streamEpoch,
        throttleGuard,
    ]);
}

