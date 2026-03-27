import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { createActor } from 'xstate';
import { getStreamChannelVersion, streamChannelMachine } from './streamChannel.machine';

describe('streamChannelMachine', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });
    afterEach(() => {
        vi.useRealTimers();
    });

    it('CONNECT_ATTEMPT then FIRST_DATA reaches running with bumped version', () => {
        const setStatus = vi.fn();
        const actor = createActor(streamChannelMachine, {
            input: { streamId: 's1', setStatus },
        });
        actor.start();
        actor.send({ type: 'CONNECT_ATTEMPT' });
        expect(actor.getSnapshot().matches('connecting')).toBe(true);
        expect(getStreamChannelVersion(actor.getSnapshot())).toBe(1);

        actor.send({ type: 'FIRST_DATA' });
        expect(actor.getSnapshot().matches('running')).toBe(true);
        expect(setStatus).toHaveBeenCalledWith('running', expect.objectContaining({ lastPacketAt: expect.any(Number) }));
        actor.stop();
    });

    it('times out after 10s in connecting', () => {
        const setStatus = vi.fn();
        const actor = createActor(streamChannelMachine, {
            input: { streamId: 's1', setStatus },
        });
        actor.start();
        actor.send({ type: 'CONNECT_ATTEMPT' });
        vi.advanceTimersByTime(10_000);
        expect(actor.getSnapshot().matches('timedOut')).toBe(true);
        expect(setStatus).toHaveBeenCalledWith(
            'timeout',
            expect.objectContaining({ message: expect.stringContaining('timed out') })
        );
        actor.stop();
    });

    it('resubscribe bumps version so old generation differs from snapshot', () => {
        const setStatus = vi.fn();
        const actor = createActor(streamChannelMachine, {
            input: { streamId: 's1', setStatus },
        });
        actor.start();
        actor.send({ type: 'CONNECT_ATTEMPT' });
        const gen1 = getStreamChannelVersion(actor.getSnapshot());
        actor.send({ type: 'FIRST_DATA' });
        expect(actor.getSnapshot().matches('running')).toBe(true);

        actor.send({ type: 'CONNECT_ATTEMPT' });
        expect(actor.getSnapshot().matches('connecting')).toBe(true);
        const gen2 = getStreamChannelVersion(actor.getSnapshot());
        expect(gen2).toBe(gen1 + 1);

        /** Simulate stale packet handler: old callback generation gen1 vs current gen2 */
        expect(gen1).not.toBe(getStreamChannelVersion(actor.getSnapshot()));
        actor.stop();
    });

    it('DATA_TICK updates running status', () => {
        const setStatus = vi.fn();
        const actor = createActor(streamChannelMachine, {
            input: { streamId: 's1', setStatus },
        });
        actor.start();
        actor.send({ type: 'CONNECT_ATTEMPT' });
        actor.send({ type: 'FIRST_DATA' });
        setStatus.mockClear();
        actor.send({ type: 'DATA_TICK' });
        expect(setStatus).toHaveBeenCalledWith('running', expect.objectContaining({ lastPacketAt: expect.any(Number) }));
        actor.stop();
    });
});
