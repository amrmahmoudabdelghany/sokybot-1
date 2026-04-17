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
    loginDetailMessage: string | null;
    gatewayResultCode: number | null;
    agentAuthResultCode: number | null;
    failureReason: string | null;
    queuePosition: number | null;
    signInComplete: boolean;
    awaitingCharacterSelection: boolean;
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
        loginDetailMessage?: string | null;
        gatewayResultCode?: number | null;
        agentAuthResultCode?: number | null;
        failureReason?: string | null;
        queuePosition?: number | null;
        signInComplete?: boolean;
        awaitingCharacterSelection?: boolean;
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
        loginDetailMessage:
            data.loginDetailMessage !== undefined
                ? (data.loginDetailMessage === null ? null : String(data.loginDetailMessage))
                : context.loginDetailMessage,
        gatewayResultCode:
            data.gatewayResultCode === undefined
                ? context.gatewayResultCode
                : typeof data.gatewayResultCode === 'number'
                    ? data.gatewayResultCode
                    : null,
        agentAuthResultCode:
            data.agentAuthResultCode === undefined
                ? context.agentAuthResultCode
                : typeof data.agentAuthResultCode === 'number'
                    ? data.agentAuthResultCode
                    : null,
        failureReason:
            data.failureReason !== undefined
                ? (data.failureReason === null ? null : String(data.failureReason))
                : context.failureReason,
        queuePosition:
            data.queuePosition === undefined
                ? context.queuePosition
                : typeof data.queuePosition === 'number'
                    ? data.queuePosition
                    : null,
        signInComplete: data.signInComplete !== undefined ? Boolean(data.signInComplete) : context.signInComplete,
        awaitingCharacterSelection:
            data.awaitingCharacterSelection !== undefined
                ? Boolean(data.awaitingCharacterSelection)
                : context.awaitingCharacterSelection,
    };
}

function isConnectPhaseValue(loginPhase: string, connected: boolean): boolean {
    return (
        loginPhase === 'DISCONNECTED'
        || loginPhase === 'MISSING_GATEWAY'
        || loginPhase === 'CONNECTING_GATEWAY'
        || loginPhase === 'GATEWAY_CONNECTED'
        || (loginPhase === 'CONNECTING_GATEWAY' && !connected)
    );
}

function isAgentPhaseValue(loginPhase: string): boolean {
    return (
        loginPhase === 'WAITING_FOR_AGENTS'
        || loginPhase === 'WAITING_FOR_AGENTS_TIMEOUT'
        || loginPhase === 'MISSING_AGENT_SERVER'
        || loginPhase === 'SERVER_INSPECTION'
        || loginPhase === 'AGENTS_RECEIVED'
        || loginPhase === 'REDIRECTING'
    );
}

function isCredentialsPhaseValue(loginPhase: string): boolean {
    return (
        loginPhase === 'MISSING_CREDENTIALS'
        || loginPhase === 'LOGIN_SENT'
        || loginPhase === 'LOGIN_SUCCESS'
        || loginPhase === 'AUTH_SENT'
        || loginPhase === 'AGENT_CONNECTED'
        || loginPhase === 'WAITING_FOR_PASSCODE'
        || loginPhase === 'WAIT_FOR_CAPTCHA'
        || loginPhase === 'PASSCODE_SUBMITTED'
        || loginPhase === 'IN_QUEUE'
        || loginPhase === 'RETRY_DELAY'
        || loginPhase === 'RETRY_DISABLED'
        || loginPhase === 'RETRY_LIMIT_REACHED'
        || loginPhase === 'FAILED'
        || loginPhase === 'MANUAL_VERIFICATION_REQUIRED'
    );
}

function isCharacterPhaseValue(loginPhase: string): boolean {
    return (
        loginPhase === 'AUTHENTICATED'
        || loginPhase === 'MISSING_CHARACTER_SELECTION'
        || loginPhase === 'LOADING_ENVIRONMENT'
    );
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
                loginDetailMessage:
                    e.loginDetailMessage !== undefined ? (e.loginDetailMessage ?? null) : context.loginDetailMessage,
                gatewayResultCode:
                    e.gatewayResultCode !== undefined ? e.gatewayResultCode : context.gatewayResultCode,
                agentAuthResultCode:
                    e.agentAuthResultCode !== undefined ? e.agentAuthResultCode : context.agentAuthResultCode,
                failureReason:
                    e.failureReason !== undefined ? (e.failureReason ?? null) : context.failureReason,
                queuePosition: e.queuePosition !== undefined ? e.queuePosition : context.queuePosition,
                signInComplete:
                    e.signInComplete !== undefined ? Boolean(e.signInComplete) : context.signInComplete,
                awaitingCharacterSelection:
                    e.awaitingCharacterSelection !== undefined
                        ? Boolean(e.awaitingCharacterSelection)
                        : context.awaitingCharacterSelection,
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
            loginDetailMessage: null,
            gatewayResultCode: null,
            agentAuthResultCode: null,
            failureReason: null,
            queuePosition: null,
            signInComplete: false,
            awaitingCharacterSelection: false,
        }),
    },
    guards: {
        isConnectPhase: ({ context }) =>
            isConnectPhaseValue(context.loginPhase, context.connected),
        isAgentPhase: ({ context }) =>
            isAgentPhaseValue(context.loginPhase),
        isCredentialsPhase: ({ context }) =>
            isCredentialsPhaseValue(context.loginPhase),
        isCharacterPhase: ({ context }) =>
            isCharacterPhaseValue(context.loginPhase),
        isInGamePhase: ({ context }) => context.loginPhase === 'IN_GAME' || context.inGame === true,
        isInGameStreamPhase: ({ event }) =>
            event.type === 'STREAM_UPDATE' && (event.loginPhase === 'IN_GAME' || event.inGame === true),
        isCharacterStreamPhase: ({ event }) =>
            event.type === 'STREAM_UPDATE' && isCharacterPhaseValue(String(event.loginPhase ?? '')),
        isCredentialsStreamPhase: ({ event }) =>
            event.type === 'STREAM_UPDATE' && isCredentialsPhaseValue(String(event.loginPhase ?? '')),
        isAgentStreamPhase: ({ event }) =>
            event.type === 'STREAM_UPDATE' && isAgentPhaseValue(String(event.loginPhase ?? '')),
        isInGameSnapshotPhase: ({ event }) =>
            event.type === 'SNAPSHOT'
            && !!event.data
            && (event.data.loginPhase === 'IN_GAME' || Boolean(event.data.inGame)),
        isCharacterSnapshotPhase: ({ event }) =>
            event.type === 'SNAPSHOT'
            && !!event.data
            && isCharacterPhaseValue(String(event.data.loginPhase ?? '')),
        isCredentialsSnapshotPhase: ({ event }) =>
            event.type === 'SNAPSHOT'
            && !!event.data
            && isCredentialsPhaseValue(String(event.data.loginPhase ?? '')),
        isAgentSnapshotPhase: ({ event }) =>
            event.type === 'SNAPSHOT'
            && !!event.data
            && isAgentPhaseValue(String(event.data.loginPhase ?? '')),
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
        loginDetailMessage: null,
        gatewayResultCode: null,
        agentAuthResultCode: null,
        failureReason: null,
        queuePosition: null,
        signInComplete: false,
        awaitingCharacterSelection: false,
    }),
    states: {
        connect: {},
        agent: {},
        credentials: {},
        character: {},
        inGame: {},
    },
    on: {
        SNAPSHOT: [
            { guard: 'isInGameSnapshotPhase', target: '.inGame', actions: 'applyCharacterSnapshot' },
            { guard: 'isCharacterSnapshotPhase', target: '.character', actions: 'applyCharacterSnapshot' },
            { guard: 'isCredentialsSnapshotPhase', target: '.credentials', actions: 'applyCharacterSnapshot' },
            { guard: 'isAgentSnapshotPhase', target: '.agent', actions: 'applyCharacterSnapshot' },
            { target: '.connect', actions: 'applyCharacterSnapshot' },
        ],
        // Backend is the source of truth; allow jump transitions to any phase bucket.
        STREAM_UPDATE: [
            { guard: 'isInGameStreamPhase', target: '.inGame', actions: 'applyStreamStatus' },
            { guard: 'isCharacterStreamPhase', target: '.character', actions: 'applyStreamStatus' },
            { guard: 'isCredentialsStreamPhase', target: '.credentials', actions: 'applyStreamStatus' },
            { guard: 'isAgentStreamPhase', target: '.agent', actions: 'applyStreamStatus' },
            { target: '.connect', actions: 'applyStreamStatus' },
        ],
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
        loginDetailMessage: ev.loginDetailMessage,
        gatewayResultCode: ev.gatewayResultCode,
        agentAuthResultCode: ev.agentAuthResultCode,
        failureReason: ev.failureReason,
        queuePosition: ev.queuePosition,
        signInComplete: ev.signInComplete,
        awaitingCharacterSelection: ev.awaitingCharacterSelection,
    };
}
