import { setup, assign } from 'xstate';
import type { CharacterState } from '../RSocketClient';
import type { MachineStatusEvent } from '../RSocketClient';

/** Sidebar onboarding / login flow context (one selected machine). */
export type OnboardingMachineContext = {
    machineId: string;
    loginPhase: string;
    connected: boolean;
    authenticated: boolean;
    inGame: boolean;
    agentOptions: Array<{ value: string; label: string }>;
    availableCharacters: string[];
    selectedCharacter: string | null;
    connectInFlight: boolean;
    // Retry timing
    retryDelayMs: number | null;
    serverTimestamp: number | null;
    retryAt: number | null;
    // Failure classification
    failureClass: string | null;
    fatal: boolean;
    uxCategory: 'CONNECT' | 'AGENT' | 'AUTH' | 'CHARACTER' | 'INGAME' | 'ERROR' | null;
    requiresInput: boolean;
    // Diagnostics
    reason: string | null;
    transition: string | null;
    topic: string | null;
    latencyMs: number | null;
};

export type OnboardingMachineEvent =
    | { type: 'SNAPSHOT'; data: CharacterState | null | undefined }
    | {
        type: 'STREAM_UPDATE';
        loginPhase?: string;
        connected?: boolean;
        authenticated?: boolean;
        inGame?: boolean;
        agentOptions?: Array<{ value: string; label: string }>;
        availableCharacters?: string[];
        selectedCharacter?: string | null;
        retryDelayMs?: number;
        serverTimestamp?: number;
        retryAt?: number;
        failureClass?: string;
        fatal?: boolean;
        uxCategory?: 'CONNECT' | 'AGENT' | 'AUTH' | 'CHARACTER' | 'INGAME' | 'ERROR';
        requiresInput?: boolean;
        reason?: string;
        transition?: string;
        topic?: string;
        latencyMs?: number;
    }
    | { type: 'CONNECT_BEGIN' }
    | { type: 'CONNECT_END' }
    | { type: 'CANCEL' };

export function showConnectCard(ctx: OnboardingMachineContext): boolean {
    return (
        ctx.loginPhase === 'MISSING_GATEWAY'
        || ctx.loginPhase === 'DISCONNECTED'
        || (ctx.loginPhase === 'CONNECTING_GATEWAY' && !ctx.connected)
    );
}

export function showAgentServerCard(ctx: OnboardingMachineContext): boolean {
    return (
        ctx.loginPhase === 'MISSING_AGENT_SERVER'
        || ctx.loginPhase === 'WAITING_FOR_AGENTS'
        || ctx.loginPhase === 'WAITING_FOR_AGENTS_TIMEOUT'
    );
}

function mergeFromCharacterState(
    context: OnboardingMachineContext,
    data: CharacterState
): Partial<OnboardingMachineContext> {
    return {
        loginPhase: data.loginPhase || context.loginPhase || 'DISCONNECTED',
        connected: Boolean(data.connected),
        authenticated: Boolean(data.authenticated),
        inGame: Boolean(data.inGame),
        agentOptions: Array.isArray(data.agentOptions) ? data.agentOptions : context.agentOptions,
        availableCharacters: Array.isArray(data.availableCharacters)
            ? data.availableCharacters
            : context.availableCharacters,
        selectedCharacter: data.selectedCharacter ?? context.selectedCharacter ?? null,
    };
}

export const machineOnboardingMachine = setup({
    types: {
        context: {} as OnboardingMachineContext,
        events: {} as OnboardingMachineEvent,
        input: {} as { machineId: string },
    },
    actions: {
        applyCharacterSnapshot: assign(({ context, event }) => {
            if (event.type !== 'SNAPSHOT' || !event.data) {
                return {};
            }
            return mergeFromCharacterState(context, event.data);
        }),
        applyStreamStatus: assign(({ context, event }) => {
            if (event.type !== 'STREAM_UPDATE') {
                return {};
            }
            const e = event;
            return {
                loginPhase:
                    e.loginPhase !== undefined && e.loginPhase !== ''
                        ? e.loginPhase
                        : (context.loginPhase || 'DISCONNECTED'),
                connected: e.connected !== undefined ? Boolean(e.connected) : context.connected,
                authenticated: e.authenticated !== undefined ? Boolean(e.authenticated) : context.authenticated,
                inGame: e.inGame !== undefined ? Boolean(e.inGame) : context.inGame,
                agentOptions: Array.isArray(e.agentOptions) ? e.agentOptions : context.agentOptions,
                availableCharacters: Array.isArray(e.availableCharacters)
                    ? e.availableCharacters
                    : context.availableCharacters,
                selectedCharacter:
                    e.selectedCharacter !== undefined && e.selectedCharacter !== null
                        ? String(e.selectedCharacter)
                        : (e.selectedCharacter === null ? null : context.selectedCharacter),
                // Retry timing
                retryDelayMs: e.retryDelayMs !== undefined ? e.retryDelayMs : null,
                serverTimestamp: e.serverTimestamp !== undefined ? e.serverTimestamp : null,
                retryAt: e.retryAt !== undefined ? e.retryAt : null,
                // Failure classification
                failureClass: e.failureClass !== undefined ? e.failureClass : null,
                fatal: e.fatal !== undefined ? Boolean(e.fatal) : false,
                uxCategory: e.uxCategory !== undefined ? e.uxCategory : context.uxCategory,
                requiresInput: e.requiresInput !== undefined ? Boolean(e.requiresInput) : context.requiresInput,
                // Diagnostics
                reason: e.reason !== undefined ? (e.reason ?? null) : context.reason,
                transition: e.transition !== undefined ? (e.transition ?? null) : context.transition,
                topic: e.topic !== undefined ? (e.topic ?? null) : context.topic,
                latencyMs: e.latencyMs !== undefined ? e.latencyMs : context.latencyMs,
            };
        }),
        setConnectInFlightOn: assign({ connectInFlight: true }),
        setConnectInFlightOff: assign({ connectInFlight: false }),
        applyCancelState: assign({
            loginPhase: 'DISCONNECTED',
            connected: false,
            authenticated: false,
            inGame: false,
            connectInFlight: false,
        }),
    },
    guards: {
        isConnectPhase: ({ context }) =>
            context.loginPhase === 'DISCONNECTED'
            || context.loginPhase === 'MISSING_GATEWAY'
            || context.loginPhase === 'CONNECTING_GATEWAY',
        isAgentPhase: ({ context }) =>
            context.loginPhase === 'WAITING_FOR_AGENTS'
            || context.loginPhase === 'WAITING_FOR_AGENTS_TIMEOUT'
            || context.loginPhase === 'MISSING_AGENT_SERVER'
            || context.loginPhase === 'SERVER_INSPECTION',
        isCredentialsPhase: ({ context }) =>
            context.loginPhase === 'MISSING_CREDENTIALS'
            || context.loginPhase === 'LOGIN_SENT'
            || context.loginPhase === 'WAITING_FOR_PASSCODE'
            || context.loginPhase === 'IN_QUEUE'
            || context.loginPhase === 'RETRY_DELAY'
            || context.loginPhase === 'RETRY_DISABLED'
            || context.loginPhase === 'RETRY_LIMIT_REACHED'
            || context.loginPhase === 'FAILED'
            || context.loginPhase === 'MANUAL_VERIFICATION_REQUIRED',
        isCharacterPhase: ({ context }) =>
            context.loginPhase === 'AUTHENTICATED'
            || context.loginPhase === 'MISSING_CHARACTER_SELECTION'
            || context.loginPhase === 'LOADING_ENVIRONMENT',
        isInGamePhase: ({ context }) => context.loginPhase === 'IN_GAME' || context.inGame === true,
    },
}).createMachine({
    id: 'machineOnboarding',
    initial: 'connect',
    context: ({ input }) => ({
        machineId: input.machineId,
        loginPhase: 'DISCONNECTED',
        connected: false,
        authenticated: false,
        inGame: false,
        agentOptions: [],
        availableCharacters: [],
        selectedCharacter: null,
        connectInFlight: false,
        retryDelayMs: null,
        serverTimestamp: null,
        retryAt: null,
        failureClass: null,
        fatal: false,
        uxCategory: null,
        requiresInput: false,
        reason: null,
        transition: null,
        topic: null,
        latencyMs: null,
    }),
    states: {
        connect: {},
        agent: {},
        credentials: {},
        character: {},
        inGame: {},
    },
    always: [
        { guard: 'isInGamePhase', target: '.inGame' },
        { guard: 'isCharacterPhase', target: '.character' },
        { guard: 'isCredentialsPhase', target: '.credentials' },
        { guard: 'isAgentPhase', target: '.agent' },
        { target: '.connect' },
    ],
    on: {
        SNAPSHOT: { actions: 'applyCharacterSnapshot' },
        // Backend is the source of truth; allow jump transitions to any phase bucket.
        STREAM_UPDATE: { actions: 'applyStreamStatus' },
        CONNECT_BEGIN: { actions: 'setConnectInFlightOn' },
        CONNECT_END: { actions: 'setConnectInFlightOff' },
        CANCEL: { actions: 'applyCancelState', target: '.connect' },
    },
});

/** Map RSocket machine status stream payload to a STREAM_UPDATE event. */
export function streamEventToMachineEvent(ev: MachineStatusEvent): OnboardingMachineEvent {
    return {
        type: 'STREAM_UPDATE',
        loginPhase: ev.loginPhase,
        connected: ev.connected,
        authenticated: ev.authenticated,
        inGame: ev.inGame,
        agentOptions: ev.agentOptions,
        availableCharacters: ev.availableCharacters,
        selectedCharacter: ev.selectedCharacter,
        retryDelayMs: ev.retryDelayMs,
        serverTimestamp: ev.serverTimestamp,
        retryAt: ev.retryAt,
        failureClass: ev.failureClass,
        fatal: ev.fatal,
        uxCategory: ev.uxCategory,
        requiresInput: ev.requiresInput,
        reason: ev.reason,
        transition: ev.transition,
        topic: ev.topic,
        latencyMs: ev.latencyMs,
    };
}
