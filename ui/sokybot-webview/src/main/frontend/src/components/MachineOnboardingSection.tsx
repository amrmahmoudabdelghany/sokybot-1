import React, { useCallback, useEffect, useState } from 'react';
import { useMachine } from '@xstate/react';
import { CharacterStatus } from '../CharacterStatus';
import { rsocketService } from '../RSocketClient';
import { MachineOnboardingPanel } from './MachineOnboardingPanel';
import { useInvalidateSokybotQueries } from '../query/sokybotQueries';
import {
    machineOnboardingMachine,
    showAgentServerCard,
    showConnectCard,
    streamEventToMachineEvent,
} from '../machines/machineOnboarding.machine';
import { cn } from '@sokybot/frontend-shared';

interface MachineOnboardingSectionProps {
    machineId: string;
}

export const MachineOnboardingSection: React.FC<MachineOnboardingSectionProps> = ({ machineId }) => {
    const { invalidateMachines } = useInvalidateSokybotQueries();
    const [state, send] = useMachine(machineOnboardingMachine, { input: { machineId } });

    const [onboardingFormByMachine, setOnboardingFormByMachine] = useState<Record<string, {
        targetGateway: string;
        username: string;
        password: string;
        passcode: string;
        targetAgent: string;
        selectedCharacter: string;
    }>>({});

    const ctx = state.context;
    const currentOnboardingForm = onboardingFormByMachine[machineId] || {
        targetGateway: '',
        username: '',
        password: '',
        passcode: '',
        targetAgent: '',
        selectedCharacter: '',
    };

    const parseMachineParts = useCallback((id: string) => {
        const parts = id.split('.', 2);
        if (parts.length < 2) return null;
        return { group: parts[0], name: parts[1] };
    }, []);

    const refreshMachineStatusSnapshot = useCallback(async (id: string) => {
        try {
            const data = await rsocketService.getCharacterState(id);
            send({ type: 'SNAPSHOT', data });
            const saved = data?.savedLogin;
            const savedGateway = saved && typeof saved.targetGateway === 'string' ? saved.targetGateway : '';
            const savedAgent = saved && typeof saved.targetAgent === 'string' ? saved.targetAgent : '';
            setOnboardingFormByMachine((prev) => ({
                ...prev,
                [id]: {
                    targetGateway: prev[id]?.targetGateway || savedGateway,
                    username: prev[id]?.username || '',
                    password: prev[id]?.password || '',
                    passcode: prev[id]?.passcode || '',
                    targetAgent: prev[id]?.targetAgent || savedAgent || String(data?.agentOptions?.[0]?.value || ''),
                    selectedCharacter:
                        prev[id]?.selectedCharacter
                        || String(data?.selectedCharacter || data?.availableCharacters?.[0] || ''),
                },
            }));
        } catch (err) {
            console.error('Failed to refresh machine status snapshot', err);
        }
    }, [send]);

    useEffect(() => {
        void refreshMachineStatusSnapshot(machineId);
        const sub = rsocketService.subscribeToMachineStatus(
            machineId,
            (statusEvent) => {
                if (!statusEvent || statusEvent.machineId !== machineId) return;
                send(streamEventToMachineEvent(statusEvent));
                void refreshMachineStatusSnapshot(machineId);
            },
            (err) => console.error('Layout machine status stream error', err)
        );
        const intervalId = window.setInterval(() => {
            void refreshMachineStatusSnapshot(machineId);
        }, 4000);
        return () => {
            window.clearInterval(intervalId);
            if (sub && typeof sub.unsubscribe === 'function') {
                sub.unsubscribe();
            }
        };
    }, [machineId, refreshMachineStatusSnapshot, send]);

    const saveLoginPayload = async (id: string, payload: Record<string, unknown>, startAfterSave?: boolean) => {
        const parts = parseMachineParts(id);
        if (!parts) return;
        if (startAfterSave) {
            send({ type: 'CONNECT_BEGIN' });
        }
        try {
            await rsocketService.initializeMachine(parts.group, parts.name, 'login', payload);
            if (startAfterSave) {
                await rsocketService.startBot(id);
                void invalidateMachines();
            }
            await refreshMachineStatusSnapshot(id);
        } finally {
            if (startAfterSave) {
                send({ type: 'CONNECT_END' });
            }
        }
    };

    const connectInFlightByMachine = ctx.connectInFlight ? { [machineId]: true } : {};

    const currentMachineStatus = {
        loginPhase: ctx.loginPhase,
        connected: ctx.connected,
        authenticated: ctx.authenticated,
        inGame: ctx.inGame,
        agentOptions: ctx.agentOptions,
        availableCharacters: ctx.availableCharacters,
        selectedCharacter: ctx.selectedCharacter,
    };

    return (
        <div className="w-80 border-l border-border/40 bg-card/80 backdrop-blur-md flex flex-col transition-all duration-300 z-20 shadow-[-4px_0_20px_rgba(0,0,0,0.02)]">
            <div className="h-20 px-4 py-2 border-b border-border/40 bg-card/60">
                <div className="w-full h-full flex flex-col justify-between">
                    <div className="w-full flex items-center justify-between text-[10px]">
                        <span className="font-bold uppercase tracking-widest text-muted-foreground">Status</span>
                        <span className="font-mono text-foreground/90">{ctx.loginPhase}</span>
                    </div>
                    <div className="w-full grid grid-cols-2 gap-2">
                        <span className={cn(
                            'inline-flex items-center justify-center gap-1.5 rounded-md px-2 py-1 text-[10px] font-semibold border',
                            ctx.connected
                                ? 'bg-emerald-600/15 text-emerald-400 border-emerald-500/30'
                                : 'bg-secondary/70 text-secondary-foreground border-border'
                        )}>
                            <span className={cn(
                                'h-1.5 w-1.5 rounded-full',
                                ctx.connected ? 'bg-emerald-400' : 'bg-muted-foreground/60'
                            )} />
                            <span>Connection</span>
                            <span className="font-mono">{ctx.connected ? 'ON' : 'OFF'}</span>
                        </span>
                        <span className={cn(
                            'inline-flex items-center justify-center gap-1.5 rounded-md px-2 py-1 text-[10px] font-semibold border',
                            ctx.authenticated
                                ? 'bg-primary/15 text-primary border-primary/30'
                                : 'bg-secondary/70 text-secondary-foreground border-border'
                        )}>
                            <span className={cn(
                                'h-1.5 w-1.5 rounded-full',
                                ctx.authenticated ? 'bg-primary' : 'bg-muted-foreground/60'
                            )} />
                            <span>Auth</span>
                            <span className="font-mono">{ctx.authenticated ? 'OK' : 'WAIT'}</span>
                        </span>
                    </div>
                </div>
            </div>
            <div className="flex-1 overflow-y-auto p-4">
                <div className="space-y-4">
                    <MachineOnboardingPanel
                        selectedMachineId={machineId}
                        currentMachineStatus={currentMachineStatus}
                        showConnectCard={showConnectCard(ctx)}
                        showAgentServerCard={showAgentServerCard(ctx)}
                        currentOnboardingForm={currentOnboardingForm}
                        connectInFlightByMachine={connectInFlightByMachine}
                        saveLoginPayload={saveLoginPayload}
                    />

                    {ctx.inGame && (
                        <CharacterStatus machineId={machineId} />
                    )}
                </div>
            </div>
        </div>
    );
};
