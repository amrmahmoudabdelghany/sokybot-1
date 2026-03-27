import { setup } from 'xstate';

/**
 * Minimal transport lifecycle for extension pages: notifies when the RSocket extension
 * subscription scope is active vs torn down. Flush/stale timers remain in the view;
 * this machine centralizes connect/disconnect semantics.
 */
export const streamTransportMachine = setup({
    types: {
        context: {} as { notify: (state: 'connected' | 'disconnected') => void },
        events: {} as { type: 'CONNECT' } | { type: 'DISCONNECT' },
        input: {} as { notify: (state: 'connected' | 'disconnected') => void },
    },
}).createMachine({
    id: 'streamTransport',
    context: ({ input }) => ({ notify: input.notify }),
    initial: 'disconnected',
    states: {
        disconnected: {
            on: {
                CONNECT: {
                    target: 'connected',
                    actions: ({ context }) => {
                        context.notify('connected');
                    },
                },
            },
        },
        connected: {
            on: {
                DISCONNECT: {
                    target: 'disconnected',
                    actions: ({ context }) => {
                        context.notify('disconnected');
                    },
                },
            },
        },
    },
});
