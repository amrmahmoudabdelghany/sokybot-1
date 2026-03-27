import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { createActor } from 'xstate';
import { extensionPageMachine, type ExtensionPageInput } from './extensionPage.machine';
import { rsocketService } from '../RSocketClient';

vi.mock('../RSocketClient', () => ({
    rsocketService: {
        subscribeToExtensionEvents: vi.fn(() => ({
            unsubscribe: vi.fn(),
            request: vi.fn(),
        })),
    },
}));

const testInput = (overrides: Partial<ExtensionPageInput> = {}): ExtensionPageInput => ({
    pageId: 'TestPage',
    onExtensionDelta: vi.fn(),
    onFlush: vi.fn(),
    onStaleCheck: vi.fn(),
    onTransportConnect: vi.fn(),
    onTransportDisconnect: vi.fn(),
    onStreamStatus: vi.fn(),
    ...overrides,
});

describe('extensionPageMachine', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });
    afterEach(() => {
        vi.useRealTimers();
        vi.mocked(rsocketService.subscribeToExtensionEvents).mockReset();
        vi.mocked(rsocketService.subscribeToExtensionEvents).mockImplementation(() => ({
            unsubscribe: vi.fn(),
            request: vi.fn(),
        }));
    });

    it('ACTIVATE calls onTransportConnect; DEACTIVATE calls onTransportDisconnect', () => {
        const onTransportConnect = vi.fn();
        const onTransportDisconnect = vi.fn();
        const onFlush = vi.fn();
        const onStaleCheck = vi.fn();

        const actor = createActor(extensionPageMachine, {
            input: testInput({ onFlush, onStaleCheck, onTransportConnect, onTransportDisconnect }),
        });
        actor.start();
        expect(actor.getSnapshot().matches('inactive')).toBe(true);

        actor.send({ type: 'ACTIVATE' });
        expect(onTransportConnect).toHaveBeenCalledTimes(1);
        expect(actor.getSnapshot().matches('active')).toBe(true);

        actor.send({ type: 'DEACTIVATE' });
        expect(onTransportDisconnect).toHaveBeenCalledTimes(1);
        expect(actor.getSnapshot().matches('inactive')).toBe(true);
        actor.stop();
    });

    it('invokes onFlush every 75ms while active', () => {
        const onFlush = vi.fn();
        const actor = createActor(extensionPageMachine, {
            input: testInput({ onFlush }),
        });
        actor.start();
        actor.send({ type: 'ACTIVATE' });
        onFlush.mockClear();
        vi.advanceTimersByTime(75);
        expect(onFlush).toHaveBeenCalledTimes(1);
        vi.advanceTimersByTime(150);
        expect(onFlush).toHaveBeenCalledTimes(3);
        actor.stop();
    });

    it('invokes onStaleCheck every 1s while active', () => {
        const onStaleCheck = vi.fn();
        const actor = createActor(extensionPageMachine, {
            input: testInput({ onStaleCheck }),
        });
        actor.start();
        actor.send({ type: 'ACTIVATE' });
        onStaleCheck.mockClear();
        vi.advanceTimersByTime(1000);
        expect(onStaleCheck).toHaveBeenCalledTimes(1);
        vi.advanceTimersByTime(2000);
        expect(onStaleCheck).toHaveBeenCalledTimes(3);
        actor.stop();
    });

    it('STREAM_ERROR disconnects transport only when every active stream has errored', () => {
        const onTransportDisconnect = vi.fn();
        const actor = createActor(extensionPageMachine, {
            input: testInput({ onTransportDisconnect }),
        });
        actor.start();
        actor.send({ type: 'ACTIVATE' });
        onTransportDisconnect.mockClear();

        actor.send({ type: 'STREAM_SUBSCRIBE_START', streamId: 'a' });
        actor.send({ type: 'STREAM_SUBSCRIBE_START', streamId: 'b' });

        actor.send({ type: 'STREAM_ERROR', streamId: 'a' });
        expect(onTransportDisconnect).not.toHaveBeenCalled();

        actor.send({ type: 'STREAM_ERROR', streamId: 'b' });
        expect(onTransportDisconnect).toHaveBeenCalledTimes(1);
        actor.stop();
    });

    it('STREAM_SUBSCRIBE_START with reconnectTransport calls onTransportConnect', () => {
        const onTransportConnect = vi.fn();
        const actor = createActor(extensionPageMachine, {
            input: testInput({ onTransportConnect }),
        });
        actor.start();
        actor.send({ type: 'ACTIVATE' });
        onTransportConnect.mockClear();

        actor.send({
            type: 'STREAM_SUBSCRIBE_START',
            streamId: 's',
            reconnectTransport: true,
        });
        expect(onTransportConnect).toHaveBeenCalledTimes(1);
        actor.stop();
    });

    it('extensionEvents invokes subscribe while active and unsubscribe on DEACTIVATE', () => {
        const unsubscribe = vi.fn();
        vi.mocked(rsocketService.subscribeToExtensionEvents).mockReturnValue({
            unsubscribe,
            request: vi.fn(),
        });
        const onExtensionDelta = vi.fn();

        const actor = createActor(extensionPageMachine, {
            input: testInput({ onExtensionDelta, pageId: 'PageA' }),
        });
        actor.start();
        actor.send({ type: 'ACTIVATE' });
        expect(rsocketService.subscribeToExtensionEvents).toHaveBeenCalledTimes(1);

        actor.send({ type: 'DEACTIVATE' });
        expect(unsubscribe).toHaveBeenCalledTimes(1);
        actor.stop();
    });

    it('STREAM_CHANNEL_ENSURE registers ids and DEACTIVATE clears them', () => {
        const actor = createActor(extensionPageMachine, {
            input: testInput(),
        });
        actor.start();
        actor.send({ type: 'ACTIVATE' });
        actor.send({ type: 'STREAM_CHANNEL_ENSURE', streamId: 's1' });
        expect(actor.getSnapshot().context.registeredStreamIds.has('s1')).toBe(true);

        actor.send({ type: 'STREAM_CHANNEL_ENSURE', streamId: 's1' });
        expect(actor.getSnapshot().context.registeredStreamIds.size).toBe(1);

        actor.send({ type: 'DEACTIVATE' });
        expect(actor.getSnapshot().context.registeredStreamIds.size).toBe(0);
        actor.stop();
    });
});
