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
        };
        expect(showAgentServerCard(ctx)).toBe(true);
    });

    it('accepts backend jump to any phase bucket', () => {
        const actor = createActor(machineOnboardingMachine, {
            input: { machineId: 'Group.bot1' },
        });
        actor.start();
        actor.send({ type: 'STREAM_UPDATE', loginPhase: 'WAITING_FOR_AGENTS' });
        expect(actor.getSnapshot().value).toBe('agent');
        actor.send({ type: 'STREAM_UPDATE', loginPhase: 'LOGIN_SENT' });
        expect(actor.getSnapshot().value).toBe('credentials');
        actor.send({ type: 'STREAM_UPDATE', loginPhase: 'IN_GAME', inGame: true });
        expect(actor.getSnapshot().value).toBe('inGame');
        actor.stop();
    });

    it('CANCEL transitions back to connect bucket', () => {
        const actor = createActor(machineOnboardingMachine, {
            input: { machineId: 'Group.bot1' },
        });
        actor.start();
        actor.send({ type: 'STREAM_UPDATE', loginPhase: 'LOGIN_SENT', connected: true });
        expect(actor.getSnapshot().value).toBe('credentials');
        actor.send({ type: 'CANCEL' });
        expect(actor.getSnapshot().value).toBe('connect');
        expect(actor.getSnapshot().context.connected).toBe(false);
        actor.stop();
    });

    it('preserves ux hint fields from stream payload', () => {
        const actor = createActor(machineOnboardingMachine, {
            input: { machineId: 'Group.bot1' },
        });
        actor.start();
        actor.send({
            type: 'STREAM_UPDATE',
            loginPhase: 'SERVER_MAINTENANCE',
            uxCategory: 'AGENT',
            requiresInput: false,
            fatal: true,
        });
        const snapshot = actor.getSnapshot().context;
        expect(snapshot.uxCategory).toBe('AGENT');
        expect(snapshot.requiresInput).toBe(false);
        expect(snapshot.fatal).toBe(true);
        actor.stop();
    });
});
