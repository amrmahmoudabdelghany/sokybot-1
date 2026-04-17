import React, { useCallback, useEffect, useState, useRef } from 'react';
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
import { resolvePhaseToUX } from '../machines/loginPhaseMapping';
import { useStablePhase } from '../machines/useStablePhase';
import { useRetryCountdown } from '../machines/useRetryCountdown';
import { useStreamThrottleGuard } from '../machines/useStreamThrottleGuard';
import { useMachineSeverityStore } from '../machines/useMachineSeverityStore';
import { cn } from '@sokybot/frontend-shared';

interface MachineOnboardingSectionProps {
    machineId: string;
}

/** Phases where we still poll character.state so agentOptions stay in sync with the model. */
const AGENT_WAIT_LOGIN_PHASES = new Set([
    'WAITING_FOR_AGENTS',
    'WAITING_FOR_AGENTS_TIMEOUT',
    'MISSING_AGENT_SERVER',
    'AGENTS_RECEIVED',
    'REDIRECTING',
    'SERVER_INSPECTION',
]);
const CONNECT_RPC_TIMEOUT_MS = 20_000;

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
        selectedCharacterSlot?: number;
        characterSlotBase?: number;
        characterSelectionStrictMode?: boolean;
        agentWaitTimeoutMs?: number;
        loginResponseTimeoutMs?: number;
        agentAuthTimeoutMs?: number;
        passcodeWaitTimeoutMs?: number;
        passcodeUserInputTimeoutMs?: number;
    }>>({});

    const [abortInFlightByMachine, setAbortInFlightByMachine] = useState<Record<string, boolean>>({});
    const abortInFlightRef = useRef<Record<string, boolean>>({});
    /** Bumped on each connect attempt and on cancel; stale async work must not apply success side effects. */
    const connectOperationGenRef = useRef(0);

    const [isOffline, setIsOffline] = useState(() => typeof navigator !== 'undefined' ? !navigator.onLine : false);
    const streamSessionIdRef = useRef<number>(0);
    const terminalPhaseGateUntilRef = useRef<number>(0);
    const machineRemovedRef = useRef(false);
    const lastStreamEventAtRef = useRef<number>(Date.now());
    const streamReconnectAttemptRef = useRef(0);
    const streamResubscribePendingRef = useRef(false);
    const [streamEpoch, setStreamEpoch] = useState(0);

    const [stableInGame, setStableInGame] = useState(false);

    const loginPhaseRef = useRef<string>(state.context.loginPhase);
    const lastHeartbeatSnapshotAtRef = useRef<number>(0);
    const gameEventsSessionIdRef = useRef(0);

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
    loginPhaseRef.current = ctx.loginPhase;

    // --- Delay hiding the onboarding panel until inGame has been stable for 2s ---
    useEffect(() => {
        if (ctx.inGame) {
            const timer = setTimeout(() => setStableInGame(true), 2000);
            return () => clearTimeout(timer);
        } else {
            setStableInGame(false);
        }
    }, [ctx.inGame]);

    // --- Derive UX model ---
    const ux = resolvePhaseToUX({
        loginPhase: ctx.loginPhase,
        uxCategory: ctx.uxCategory,
        requiresInput: ctx.requiresInput,
        fatal: ctx.fatal,
    });

    // --- Anti-flicker debounce (errors immediate, others 400ms) ---
    const { stable: stableTitle } = useStablePhase(ux.displayTitle, ux.severity, machineId);

    // --- Propagate severity to global sidebar ---
    const setSeverity = useMachineSeverityStore((s) => s.setSeverity);
    const clearMachine = useMachineSeverityStore((s) => s.clearMachine);
    useEffect(() => {
        setSeverity(machineId, ux.severity);
    }, [machineId, ux.severity, setSeverity]);
    useEffect(() => {
        return () => clearMachine(machineId);
    }, [machineId, clearMachine]);

    // --- Stream thrashing guard ---
    const throttleGuard = useStreamThrottleGuard();
    const currentOnboardingForm = onboardingFormByMachine[machineId] || {
        targetGateway: '',
        username: '',
        password: '',
        passcode: '',
        targetAgent: '',
        selectedCharacter: '',
        selectedCharacterSlot: -1,
        characterSlotBase: 0,
        characterSelectionStrictMode: false,
        agentWaitTimeoutMs: 15000,
        loginResponseTimeoutMs: 15000,
        agentAuthTimeoutMs: 30000,
        passcodeWaitTimeoutMs: 60000,
        passcodeUserInputTimeoutMs: 60000,
    };

    const parseMachineParts = useCallback((id: string) => {
        const parts = id.split('.', 2);
        if (parts.length < 2) return null;
        return { group: parts[0], name: parts[1] };
    }, []);

    const isHighPriorityPhase = useCallback((phase?: string) => {
        if (!phase) return false;
        if (phase.startsWith('MISSING_')) return true;
        return (
            phase === 'FAILED'
            || phase === 'RETRY_DELAY'
            || phase === 'RETRY_DISABLED'
            || phase === 'RETRY_LIMIT_REACHED'
            || phase === 'WAITING_FOR_PASSCODE'
            || phase === 'IN_QUEUE'
            || phase === 'SERVER_INSPECTION'
        );
    }, []);

    const refreshMachineStatusSnapshot = useCallback(async (id: string, attempt = 0) => {
        try {
            const data = await rsocketService.getCharacterState(id);
            send({ type: 'SNAPSHOT', data });
            const saved = data?.savedLogin;
            const savedGateway = saved && typeof saved.targetGateway === 'string' ? saved.targetGateway : '';
            const savedAgent = saved && typeof saved.targetAgent === 'string' ? saved.targetAgent : '';
            setOnboardingFormByMachine((prev) => ({
                ...prev,
                [id]: {
                    targetGateway: savedGateway,
                    username: saved && typeof saved.username === 'string' ? saved.username : (prev[id]?.username || ''),
                    password: saved && typeof saved.password === 'string' ? saved.password : (prev[id]?.password || ''),
                    passcode: saved && typeof saved.passcode === 'string' ? saved.passcode : (prev[id]?.passcode || ''),
                    targetAgent: savedAgent || String(data?.agentOptions?.[0]?.value || ''),
                    selectedCharacter: String(data?.selectedCharacter || data?.availableCharacters?.[0] || ''),
                    selectedCharacterSlot: saved && typeof saved.selectedCharacterSlot === 'number' ? saved.selectedCharacterSlot : (prev[id]?.selectedCharacterSlot ?? -1),
                    characterSlotBase: saved && typeof saved.characterSlotBase === 'number' ? saved.characterSlotBase : (prev[id]?.characterSlotBase ?? 0),
                    characterSelectionStrictMode: saved && typeof saved.characterSelectionStrictMode === 'boolean' ? saved.characterSelectionStrictMode : (prev[id]?.characterSelectionStrictMode ?? false),
                    agentWaitTimeoutMs: saved && typeof saved.agentWaitTimeoutMs === 'number' ? saved.agentWaitTimeoutMs : (prev[id]?.agentWaitTimeoutMs ?? 15000),
                    loginResponseTimeoutMs: saved && typeof saved.loginResponseTimeoutMs === 'number' ? saved.loginResponseTimeoutMs : (prev[id]?.loginResponseTimeoutMs ?? 15000),
                    agentAuthTimeoutMs: saved && typeof saved.agentAuthTimeoutMs === 'number' ? saved.agentAuthTimeoutMs : (prev[id]?.agentAuthTimeoutMs ?? 30000),
                    passcodeWaitTimeoutMs: saved && typeof saved.passcodeWaitTimeoutMs === 'number' ? saved.passcodeWaitTimeoutMs : (prev[id]?.passcodeWaitTimeoutMs ?? 60000),
                    passcodeUserInputTimeoutMs: saved && typeof saved.passcodeUserInputTimeoutMs === 'number' ? saved.passcodeUserInputTimeoutMs : (prev[id]?.passcodeUserInputTimeoutMs ?? 60000),
                },
            }));
        } catch (err) {
            console.error('Failed to refresh machine status snapshot', err);
            if (attempt < 3) {
                const base = Math.pow(2, attempt) * 1000;
                const jitter = Math.floor(Math.random() * 1500);
                setTimeout(() => refreshMachineStatusSnapshot(id, attempt + 1), base + jitter);
            }
        }
    }, [send]);

    // Refresh onboarding snapshot when agent list arrives (CharacterStatus is not mounted until inGame).
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

    useEffect(() => {
        // Increment session ID on each mount/reconnect to invalidate stale events
        const currentSessionId = ++streamSessionIdRef.current;
        machineRemovedRef.current = false;
        lastStreamEventAtRef.current = Date.now();
        streamReconnectAttemptRef.current = 0;
        streamResubscribePendingRef.current = false;

        void refreshMachineStatusSnapshot(machineId);
        const sub = rsocketService.subscribeToMachineStatus(
            machineId,
            (statusEvent) => {
                // Stale event guard
                if (streamSessionIdRef.current !== currentSessionId) return;

                if (!statusEvent || statusEvent.machineId !== machineId) return;

                if (machineRemovedRef.current) {
                    return;
                }

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

                // Cancellation race condition guard
                if (
                    abortInFlightRef.current[statusEvent.machineId]
                    && statusEvent.loginPhase !== 'FAILED'
                    && statusEvent.loginPhase !== 'DISCONNECTED'
                ) {
                    console.log(`[${statusEvent.machineId}] Suppressing intermediate phase ${statusEvent.loginPhase} during abort`);
                    return;
                }

                // If a terminal/high-priority phase arrived recently, ignore stale in-progress regressions.
                if (
                    terminalPhaseGateUntilRef.current > Date.now()
                    && !isHighPriorityPhase(statusEvent.loginPhase)
                ) {
                    void refreshMachineStatusSnapshot(machineId);
                    return;
                }

                // Stream thrashing protection
                if (!isHighPriorityPhase(statusEvent.loginPhase) && throttleGuard.recordTransition()) {
                    console.warn(`[${machineId}] Stream thrashing detected – transitions throttled`);
                    void refreshMachineStatusSnapshot(machineId);
                    return;
                }
                if (isHighPriorityPhase(statusEvent.loginPhase)) {
                    // Cancel/flush pending low-priority lag by blocking stale phases briefly.
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
    }, [isHighPriorityPhase, machineId, refreshMachineStatusSnapshot, send, streamEpoch, throttleGuard]);

    const saveLoginPayload = async (id: string, payload: Record<string, unknown>, startAfterSave?: boolean) => {
        const parts = parseMachineParts(id);
        if (!parts) return;
        setOnboardingFormByMachine((prev) => ({
            ...prev,
            [id]: {
                ...(prev[id] || {
                    targetGateway: '',
                    username: '',
                    password: '',
                    passcode: '',
                    targetAgent: '',
                    selectedCharacter: '',
                    selectedCharacterSlot: -1,
                    characterSlotBase: 0,
                    characterSelectionStrictMode: false,
                    agentWaitTimeoutMs: 15000,
                    loginResponseTimeoutMs: 15000,
                    agentAuthTimeoutMs: 30000,
                    passcodeWaitTimeoutMs: 60000,
                    passcodeUserInputTimeoutMs: 60000,
                }),
                ...payload,
            },
        }));
        let connectGen: number | undefined;
        if (startAfterSave) {
            connectGen = ++connectOperationGenRef.current;
            send({ type: 'CONNECT_BEGIN' });
        }
        const timeoutError = new Error(`Connect initialization timed out after ${CONNECT_RPC_TIMEOUT_MS}ms`);
        const timeoutPromise = new Promise<never>((_, reject) => {
            window.setTimeout(() => reject(timeoutError), CONNECT_RPC_TIMEOUT_MS);
        });
        try {
            await Promise.race([
                (async () => {
                    await rsocketService.initializeMachine(parts.group, parts.name, 'login', payload);
                    if (
                        startAfterSave
                        && connectGen !== undefined
                        && connectGen !== connectOperationGenRef.current
                    ) {
                        return;
                    }
                    if (startAfterSave) {
                        await rsocketService.startBot(id);
                    }
                })(),
                timeoutPromise,
            ]);
            if (startAfterSave) {
                if (connectGen !== undefined && connectGen !== connectOperationGenRef.current) {
                    return;
                }
                void invalidateMachines();
            }
            if (
                startAfterSave
                && connectGen !== undefined
                && connectGen !== connectOperationGenRef.current
            ) {
                return;
            }
            await refreshMachineStatusSnapshot(id);
        } catch (err) {
            const isTimeout = err instanceof Error && err.message === timeoutError.message;
            if (isTimeout && startAfterSave) {
                send({
                    type: 'STREAM_UPDATE',
                    loginPhase: 'FAILED',
                    reason: 'Initialization/start request timed out',
                    loginDetailMessage: 'Connection attempt timed out while initializing machine',
                    failureReason: 'connect_timeout',
                    fatal: false,
                    failureClass: 'NETWORK',
                    uxCategory: 'ERROR',
                });
            }
            console.error('Failed to save login payload', err);
        } finally {
            if (
                startAfterSave
                && connectGen !== undefined
                && connectGen === connectOperationGenRef.current
            ) {
                send({ type: 'CONNECT_END' });
            }
        }
    };

    const startMachine = async (id: string) => {
        const gen = ++connectOperationGenRef.current;
        send({ type: 'CONNECT_BEGIN' });
        try {
            await rsocketService.startBot(id);
            if (gen !== connectOperationGenRef.current) {
                return;
            }
            void invalidateMachines();
            await refreshMachineStatusSnapshot(id);
        } finally {
            if (gen === connectOperationGenRef.current) {
                send({ type: 'CONNECT_END' });
            }
        }
    };

    const abortLogin = async (id: string) => {
        connectOperationGenRef.current += 1;
        send({ type: 'CANCEL' });
        send({ type: 'CONNECT_END' });
        setAbortInFlightByMachine((prev) => {
            const next = { ...prev, [id]: true };
            abortInFlightRef.current = next;
            return next;
        });
        try {
            await rsocketService.stopBot(id);
            void refreshMachineStatusSnapshot(id);
        } catch (err) {
            console.error('Failed to abort login', err);
        } finally {
            setAbortInFlightByMachine((prev) => {
                const next = { ...prev, [id]: false };
                abortInFlightRef.current = next;
                return next;
            });
        }
    };

    const connectInFlightByMachine = ctx.connectInFlight ? { [machineId]: true } : {};

    // --- Retry countdown ---
    const retryCountdown = useRetryCountdown({
        isRetryPhase: ctx.loginPhase === 'RETRY_DELAY',
        retryAt: ctx.retryAt,
        retryDelayMs: ctx.retryDelayMs,
        serverTimestamp: ctx.serverTimestamp,
        fatal: ctx.fatal,
        isOffline: typeof navigator !== 'undefined' ? !navigator.onLine : false,
        onRetryNow: () => {
            const parts = parseMachineParts(machineId);
            if (!parts) return;
            void saveLoginPayload(machineId, { autoLogin: true }, true);
        },
    });

    const currentMachineStatus = {
        loginPhase: ctx.loginPhase,
        connected: ctx.connected,
        authenticated: ctx.authenticated,
        inGame: ctx.inGame,
        agentOptions: ctx.agentOptions,
        availableCharacters: ctx.availableCharacters,
        selectedCharacter: ctx.selectedCharacter,
        reason: ctx.reason,
        failureClass: ctx.failureClass,
        fatal: ctx.fatal,
        uxCategory: ctx.uxCategory,
        requiresInput: ctx.requiresInput,
        latencyMs: ctx.latencyMs,
        loginDetailMessage: ctx.loginDetailMessage,
        gatewayResultCode: ctx.gatewayResultCode,
        agentAuthResultCode: ctx.agentAuthResultCode,
        failureReason: ctx.failureReason,
        queuePosition: ctx.queuePosition,
    };

    return (
        <div className="w-80 border-l border-border bg-card/80 backdrop-blur-md flex flex-col min-h-0 transition-all duration-300 z-20 shadow-[-4px_0_20px_rgba(0,0,0,0.02)]">
            <div
                className="shrink-0 px-4 py-3 border-b border-border bg-card/50 flex flex-col justify-center gap-1.5"
                role="status"
                aria-live="polite"
            >
                {(() => {
                    const ux = resolvePhaseToUX({
                        loginPhase: ctx.loginPhase,
                        uxCategory: ctx.uxCategory,
                        requiresInput: ctx.requiresInput,
                        fatal: ctx.fatal,
                    });
                    // Auth badge colour/label
                    const authBadge = (() => {
                        // Failed auth takes precedence over connected state
                        if (ux.authState === 'failed') return { label: 'Failed', dotClass: 'bg-red-400', badgeClass: 'bg-red-600/15 text-red-400 border-red-500/30' };
                        if (ux.authState === 'success') return { label: 'Success', dotClass: 'bg-primary', badgeClass: 'bg-primary/15 text-primary border-primary/30' };
                        if (ux.authState === 'in_progress') return { label: 'In progress', dotClass: 'bg-amber-400 animate-pulse', badgeClass: 'bg-amber-600/15 text-amber-400 border-amber-500/30' };
                        return { label: 'Not started', dotClass: 'bg-muted-foreground/60', badgeClass: 'bg-secondary/70 text-secondary-foreground border-border' };
                    })();

                    return (
                        <>
                            {/* Step title (debounced for anti-flicker) */}
                            <div className="text-xs font-semibold text-foreground leading-tight truncate">
                                {stableTitle}
                            </div>

                            {/* Badges row */}
                            <div className="w-full grid grid-cols-2 gap-1">
                                {/* Network badge */}
                                <span className={cn(
                                    'inline-flex items-center justify-center gap-1 rounded-md px-1.5 py-0.5 text-[10px] font-semibold border',
                                    isOffline
                                        ? 'bg-destructive/15 text-destructive border-destructive/30'
                                        : ctx.connected
                                            ? 'bg-emerald-600/15 text-emerald-400 border-emerald-500/30'
                                            : 'bg-secondary/70 text-secondary-foreground border-border'
                                )}>
                                    <span className={cn(
                                        'h-1.5 w-1.5 rounded-full',
                                        isOffline ? 'bg-destructive' : ctx.connected ? 'bg-emerald-400' : 'bg-muted-foreground/60'
                                    )} />
                                    <span>
                                        {isOffline
                                            ? 'Offline'
                                            : ctx.connected
                                                ? (typeof ctx.latencyMs === 'number' && ctx.latencyMs >= 0
                                                    ? `Connected (${Math.round(ctx.latencyMs)}ms)`
                                                    : 'Connected')
                                                : 'Disconnected'}
                                    </span>
                                </span>

                                {/* Auth badge */}
                                <span className={cn(
                                    'inline-flex items-center justify-center gap-1.5 rounded-md px-2 py-0.5 text-[10px] font-semibold border',
                                    authBadge.badgeClass,
                                )}>
                                    <span className={cn('h-1.5 w-1.5 rounded-full', authBadge.dotClass)} />
                                    <span>Auth</span>
                                    <span className="font-mono">{authBadge.label}</span>
                                </span>
                            </div>

                            {/* Raw phase chip – muted, hidden at narrowest widths */}
                            <span className="hidden min-[320px]:inline-block font-mono text-[9px] text-muted-foreground/60 truncate mt-0.5">
                                {ctx.loginPhase}
                            </span>
                        </>
                    );
                })()}

                {/* Stream thrashing warning */}
                {throttleGuard.isTripped && (
                    <div className="px-4 py-2 bg-amber-500/10 border-b border-amber-500/20 flex items-center justify-between gap-2">
                        <span className="text-[10px] text-amber-400 font-medium">Too many status updates detected</span>
                        <button
                            className="text-[10px] text-primary hover:underline font-semibold"
                            onClick={() => throttleGuard.reset()}
                        >
                            Reconnect
                        </button>
                    </div>
                )}
            </div>
            <div className="flex-1 min-h-0 overflow-y-auto p-4">
                <div className="space-y-4">
                    {!stableInGame && (
                        <MachineOnboardingPanel
                            key={machineId}
                            selectedMachineId={machineId}
                            currentMachineStatus={currentMachineStatus}
                            showConnectCard={showConnectCard(ctx)}
                            showAgentServerCard={showAgentServerCard(ctx)}
                            currentOnboardingForm={currentOnboardingForm}
                            connectInFlightByMachine={connectInFlightByMachine}
                            abortInFlightByMachine={abortInFlightByMachine}
                            isOffline={isOffline}
                            saveLoginPayload={saveLoginPayload}
                            startMachine={startMachine}
                            abortLogin={abortLogin}
                            retryCountdown={retryCountdown}
                        />
                    )}

                    {ctx.inGame && (
                        <CharacterStatus machineId={machineId} />
                    )}
                </div>
            </div>
        </div>
    );
};
