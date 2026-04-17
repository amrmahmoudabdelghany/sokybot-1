import { useEffect, useState } from 'react';
import { useMachine } from '@xstate/react';
import { machineOnboardingMachine } from '../machines/machineOnboarding.machine';
import { resolvePhaseToUX } from '../machines/loginPhaseMapping';
import { useStablePhase } from '../machines/useStablePhase';
import { useMachineSeverityStore } from '../machines/useMachineSeverityStore';

export function useMachineOnboardingMachine(machineId: string) {
    const [state, send] = useMachine(machineOnboardingMachine, { input: { machineId } });
    const [isOffline, setIsOffline] = useState(() => (typeof navigator !== 'undefined' ? !navigator.onLine : false));
    const [stableInGame, setStableInGame] = useState(false);

    useEffect(() => {
        const handleOnline = () => setIsOffline(false);
        const handleOffline = () => setIsOffline(true);
        window.addEventListener('online', handleOnline);
        window.addEventListener('offline', handleOffline);
        return () => {
            window.removeEventListener('online', handleOnline);
            window.removeEventListener('offline', handleOffline);
        };
    }, []);

    const ctx = state.context;
    useEffect(() => {
        if (ctx.inGame) {
            const timer = setTimeout(() => setStableInGame(true), 2000);
            return () => clearTimeout(timer);
        }
        const timer = setTimeout(() => setStableInGame(false), 0);
        return () => clearTimeout(timer);
    }, [ctx.inGame]);

    const ux = resolvePhaseToUX({
        loginPhase: ctx.loginPhase,
        uxCategory: ctx.uxCategory,
        requiresInput: ctx.requiresInput,
        fatal: ctx.fatal,
    });
    const { stable: stableTitle } = useStablePhase(ux.displayTitle, ux.severity, machineId);

    const setSeverity = useMachineSeverityStore((s) => s.setSeverity);
    const clearMachine = useMachineSeverityStore((s) => s.clearMachine);
    useEffect(() => {
        setSeverity(machineId, ux.severity);
    }, [machineId, ux.severity, setSeverity]);
    useEffect(() => {
        return () => clearMachine(machineId);
    }, [machineId, clearMachine]);

    return {
        state,
        send,
        ctx,
        ux,
        stableTitle,
        isOffline,
        stableInGame,
    };
}

