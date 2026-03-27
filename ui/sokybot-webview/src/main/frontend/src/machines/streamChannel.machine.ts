import { assign, setup } from 'xstate';

export type StreamStatusSink = (status: string, details?: Record<string, unknown>) => void;

export type StreamChannelInput = {
    streamId: string;
    setStatus: StreamStatusSink;
};

export type StreamChannelEvent =
    | { type: 'CONNECT_ATTEMPT' }
    /** First application payload after subscribe (clears connect timeout / lock). */
    | { type: 'FIRST_DATA' }
    /** Further data packets while running (updates lastPacketAt). */
    | { type: 'DATA_TICK' }
    | { type: 'STATUS'; status: string; details?: Record<string, unknown> }
    | { type: 'TERMINATED'; reason?: string }
    | { type: 'ERROR'; message: string };

export const streamChannelMachine = setup({
    types: {
        context: {} as {
            streamId: string;
            version: number;
            setStatus: StreamStatusSink;
        },
        events: {} as StreamChannelEvent,
        input: {} as StreamChannelInput,
    },
}).createMachine({
    id: 'streamChannel',
    context: ({ input }) => ({
        streamId: input.streamId,
        version: 0,
        setStatus: input.setStatus,
    }),
    initial: 'idle',
    states: {
        idle: {
            on: {
                CONNECT_ATTEMPT: {
                    target: 'connecting',
                    actions: assign({
                        version: ({ context }) => context.version + 1,
                    }),
                },
            },
        },
        connecting: {
            entry: ({ context }) => {
                context.setStatus('connecting');
            },
            after: {
                10_000: {
                    target: 'timedOut',
                    actions: ({ context }) => {
                        context.setStatus('timeout', { message: 'Connection attempt timed out (10s)' });
                    },
                },
            },
            on: {
                FIRST_DATA: {
                    target: 'running',
                    actions: ({ context }) => {
                        context.setStatus('running', { lastPacketAt: Date.now() });
                    },
                },
                DATA_TICK: {
                    target: 'running',
                    actions: ({ context }) => {
                        context.setStatus('running', { lastPacketAt: Date.now() });
                    },
                },
                STATUS: [
                    {
                        guard: ({ event }) =>
                            event.status === 'running' || Boolean(event.details?.isSubscribed),
                        target: 'running',
                        actions: ({ context, event }) => {
                            context.setStatus(event.status, event.details);
                        },
                    },
                    {
                        actions: ({ context, event }) => {
                            context.setStatus(event.status, event.details);
                        },
                    },
                ],
                TERMINATED: {
                    target: 'disconnected',
                    actions: ({ context, event }) => {
                        context.setStatus('disconnected', { reason: event.reason || 'terminated' });
                    },
                },
                ERROR: {
                    target: 'error',
                    actions: ({ context, event }) => {
                        context.setStatus('disconnected', { error: event.message });
                    },
                },
            },
        },
        running: {
            on: {
                CONNECT_ATTEMPT: {
                    target: 'connecting',
                    actions: assign({
                        version: ({ context }) => context.version + 1,
                    }),
                },
                DATA_TICK: {
                    actions: ({ context }) => {
                        context.setStatus('running', { lastPacketAt: Date.now() });
                    },
                },
                STATUS: {
                    actions: ({ context, event }) => {
                        context.setStatus(event.status, event.details);
                    },
                },
                TERMINATED: {
                    target: 'disconnected',
                    actions: ({ context, event }) => {
                        context.setStatus('disconnected', { reason: event.reason || 'terminated' });
                    },
                },
                ERROR: {
                    target: 'error',
                    actions: ({ context, event }) => {
                        context.setStatus('disconnected', { error: event.message });
                    },
                },
            },
        },
        timedOut: {
            on: {
                CONNECT_ATTEMPT: {
                    target: 'connecting',
                    actions: assign({
                        version: ({ context }) => context.version + 1,
                    }),
                },
            },
        },
        disconnected: {
            on: {
                CONNECT_ATTEMPT: {
                    target: 'connecting',
                    actions: assign({
                        version: ({ context }) => context.version + 1,
                    }),
                },
            },
        },
        error: {
            on: {
                CONNECT_ATTEMPT: {
                    target: 'connecting',
                    actions: assign({
                        version: ({ context }) => context.version + 1,
                    }),
                },
            },
        },
    },
});

export function getStreamChannelVersion(actorSnapshot: MinChannelSnapshot): number {
    return actorSnapshot.context.version;
}

type MinChannelSnapshot = { context: { version: number } };
