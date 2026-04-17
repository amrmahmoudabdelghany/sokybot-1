import { useEffect, useRef } from 'react';
import { rsocketService } from '../RSocketClient';

type Params = {
    machineId: string;
    refreshMachineStatusSnapshot: (id: string) => Promise<void>;
};

export function useGameEventsSync({ machineId, refreshMachineStatusSnapshot }: Params) {
    const gameEventsSessionIdRef = useRef(0);

    useEffect(() => {
        const sid = ++gameEventsSessionIdRef.current;
        const minIntervalMs = 400;
        let debounceId: number | null = null;
        let lastRefreshAt = 0;

        const scheduleSnapshot = () => {
            const now = Date.now();
            const waitMs = now - lastRefreshAt >= minIntervalMs ? 0 : minIntervalMs - (now - lastRefreshAt);
            if (debounceId != null) {
                window.clearTimeout(debounceId);
            }
            debounceId = window.setTimeout(() => {
                debounceId = null;
                if (gameEventsSessionIdRef.current !== sid) {
                    return;
                }
                lastRefreshAt = Date.now();
                void refreshMachineStatusSnapshot(machineId);
            }, waitMs);
        };

        const sub = rsocketService.subscribeToGameEvents(
            (event) => {
                if (gameEventsSessionIdRef.current !== sid || !event) {
                    return;
                }
                if (String(event.eventType) !== 'AgentListEvent') {
                    return;
                }
                const eventTopicsRaw = (event as Record<string, unknown>)['event.topics'];
                const eventTopics = Array.isArray(eventTopicsRaw) ? eventTopicsRaw.map(String) : [];
                const isForMachine =
                    eventTopics.some((topic) => topic.includes(machineId))
                    || (typeof (event as Record<string, unknown>).fullName === 'string'
                        && String((event as Record<string, unknown>).fullName).includes(machineId));
                if (!isForMachine) {
                    return;
                }
                scheduleSnapshot();
            },
            (err) => console.error('Onboarding game.events stream error', err)
        );

        return () => {
            if (debounceId != null) {
                window.clearTimeout(debounceId);
            }
            if (sub && typeof sub.unsubscribe === 'function') {
                sub.unsubscribe();
            }
        };
    }, [machineId, refreshMachineStatusSnapshot]);
}

