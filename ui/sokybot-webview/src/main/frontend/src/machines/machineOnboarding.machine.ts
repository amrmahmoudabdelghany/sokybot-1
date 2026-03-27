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
      }
    | { type: 'CONNECT_BEGIN' }
    | { type: 'CONNECT_END' };

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
            };
        }),
        setConnectInFlightOn: assign({ connectInFlight: true }),
        setConnectInFlightOff: assign({ connectInFlight: false }),
    },
}).createMachine({
    id: 'machineOnboarding',
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
    }),
    on: {
        SNAPSHOT: { actions: 'applyCharacterSnapshot' },
        STREAM_UPDATE: { actions: 'applyStreamStatus' },
        CONNECT_BEGIN: { actions: 'setConnectInFlightOn' },
        CONNECT_END: { actions: 'setConnectInFlightOff' },
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
    };
}
