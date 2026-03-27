import { describe, it, expect } from 'vitest';
import { createActor } from 'xstate';
import {
    machineOnboardingMachine,
    showAgentServerCard,
    showConnectCard,
} from './machineOnboarding.machine';

describe('machineOnboardingMachine', () => {
    it('toggles connectInFlight on CONNECT_BEGIN / CONNECT_END', () => {
        const actor = createActor(machineOnboardingMachine, {
            input: { machineId: 'Group.bot1' },
        });
        actor.start();
        expect(actor.getSnapshot().context.connectInFlight).toBe(false);
        actor.send({ type: 'CONNECT_BEGIN' });
        expect(actor.getSnapshot().context.connectInFlight).toBe(true);
        actor.send({ type: 'CONNECT_END' });
        expect(actor.getSnapshot().context.connectInFlight).toBe(false);
        actor.stop();
    });

    it('showConnectCard respects CONNECTING_GATEWAY and connected flag', () => {
        const ctx = {
            machineId: 'g.m',
            loginPhase: 'CONNECTING_GATEWAY',
            connected: false,
            authenticated: false,
            inGame: false,
            agentOptions: [],
            availableCharacters: [],
            selectedCharacter: null,
            connectInFlight: false,
        };
        expect(showConnectCard(ctx)).toBe(true);
        expect(showConnectCard({ ...ctx, connected: true })).toBe(false);
    });

    it('showAgentServerCard for WAITING_FOR_AGENTS', () => {
        const ctx = {
            machineId: 'g.m',
            loginPhase: 'WAITING_FOR_AGENTS',
            connected: true,
            authenticated: false,
            inGame: false,
            agentOptions: [],
            availableCharacters: [],
            selectedCharacter: null,
            connectInFlight: false,
        };
        expect(showAgentServerCard(ctx)).toBe(true);
    });
});
