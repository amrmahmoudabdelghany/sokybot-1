import { assign, fromCallback, setup } from 'xstate';
import type { AnyActorRef } from 'xstate';
import { rsocketService } from '../RSocketClient';
import { streamChannelMachine } from './streamChannel.machine';

type RegistrySystem = { get: (key: never) => AnyActorRef | undefined };

const flushTicker = fromCallback(({ sendBack }) => {
    const id = setInterval(() => sendBack({ type: 'FLUSH_TICK' }), 75);
    return () => clearInterval(id);
});

const staleTicker = fromCallback(({ sendBack }) => {
    const id = setInterval(() => sendBack({ type: 'STALE_TICK' }), 1000);
    return () => clearInterval(id);
});

/** Subscribes to `extension.events` and forwards `${pageId}.stateChanged` deltas. */
const extensionEventsBridge = fromCallback(
    ({
        input,
    }: {
        input: { pageId: string; onExtensionDelta: (delta: Record<string, unknown>) => void };
    }) => {
        const { pageId, onExtensionDelta } = input;
        const sub = rsocketService.subscribeToExtensionEvents(
            (event) => {
                const payload = event as { type?: string; delta?: Record<string, unknown> };
                if (payload.type === `${pageId}.stateChanged` && payload.delta) {
                    onExtensionDelta(payload.delta);
                }
            },
            (error) => console.error('Extension event error', error)
        );
        return () => {
            sub.unsubscribe();
        };
    }
);

export type ExtensionPageInput = {
    pageId: string;
    onExtensionDelta: (delta: Record<string, unknown>) => void;
    onFlush: () => void;
    onStaleCheck: () => void;
    onTransportConnect: () => void;
    onTransportDisconnect: () => void;
    onStreamStatus: (streamId: string, status: string, details?: Record<string, unknown>) => void;
};

export type ExtensionPageEvent =
    | { type: 'ACTIVATE' }
    | { type: 'DEACTIVATE' }
    | { type: 'FLUSH_TICK' }
    | { type: 'STALE_TICK' }
    | { type: 'STREAM_SUBSCRIBE_START'; streamId: string; reconnectTransport?: boolean }
    | { type: 'STREAM_ERROR'; streamId: string }
    | { type: 'STREAM_CHANNEL_ENSURE'; streamId: string };

export type ExtensionPageContext = ExtensionPageInput & {
    activeStreamIds: Set<string>;
    erroredStreamIds: Set<string>;
    transportDisconnectedByAllStreamsFail: boolean;
    /** Stream ids for which a child `streamChannel` actor was spawned (look up via `system.get('stream-' + id)`). */
    registeredStreamIds: Set<string>;
};

export const extensionPageMachine = setup({
    types: {
        context: {} as ExtensionPageContext,
        events: {} as ExtensionPageEvent,
        input: {} as ExtensionPageInput,
    },
    actors: {
        flushTicker,
        staleTicker,
        streamChannel: streamChannelMachine,
        extensionEvents: extensionEventsBridge,
    },
}).createMachine({
    id: 'extensionPage',
    context: ({ input }) => ({
        pageId: input.pageId,
        onExtensionDelta: input.onExtensionDelta,
        onFlush: input.onFlush,
        onStaleCheck: input.onStaleCheck,
        onTransportConnect: input.onTransportConnect,
        onTransportDisconnect: input.onTransportDisconnect,
        onStreamStatus: input.onStreamStatus,
        activeStreamIds: new Set<string>(),
        erroredStreamIds: new Set<string>(),
        transportDisconnectedByAllStreamsFail: false,
        registeredStreamIds: new Set<string>(),
    }),
    initial: 'inactive',
    states: {
        inactive: {
            on: {
                ACTIVATE: {
                    target: 'active',
                    actions: ({ context }) => {
                        context.onTransportConnect();
                    },
                },
            },
        },
        active: {
            invoke: [
                { src: 'flushTicker', id: 'flush' },
                { src: 'staleTicker', id: 'stale' },
                {
                    src: 'extensionEvents',
                    id: 'extensionEvents',
                    input: ({ context }) => ({
                        pageId: context.pageId,
                        onExtensionDelta: context.onExtensionDelta,
                    }),
                },
            ],
            exit: [
                ({ context, self }) => {
                    const sys = self.system as RegistrySystem;
                    for (const sid of context.registeredStreamIds) {
                        const child = sys.get(`stream-${sid}` as never);
                        child?.stop();
                    }
                },
                assign({
                    registeredStreamIds: () => new Set(),
                    activeStreamIds: () => new Set(),
                    erroredStreamIds: () => new Set(),
                    transportDisconnectedByAllStreamsFail: () => false,
                }),
            ],
            on: {
                DEACTIVATE: {
                    target: 'inactive',
                    actions: ({ context }) => {
                        context.onTransportDisconnect();
                    },
                },
                FLUSH_TICK: {
                    actions: ({ context }) => {
                        context.onFlush();
                    },
                },
                STALE_TICK: {
                    actions: ({ context }) => {
                        context.onStaleCheck();
                    },
                },
                STREAM_CHANNEL_ENSURE: {
                    actions: assign(({ context, event, spawn }) => {
                        if (event.type !== 'STREAM_CHANNEL_ENSURE') return {};
                        const sid = event.streamId;
                        if (context.registeredStreamIds.has(sid)) return {};
                        spawn('streamChannel', {
                            id: `stream-${sid}`,
                            input: {
                                streamId: sid,
                                setStatus: (status, details) =>
                                    context.onStreamStatus(sid, status, details),
                            },
                        });
                        const next = new Set(context.registeredStreamIds);
                        next.add(sid);
                        return { registeredStreamIds: next };
                    }),
                },
                STREAM_SUBSCRIBE_START: {
                    actions: [
                        assign(({ context, event }) => {
                            const active = new Set(context.activeStreamIds);
                            active.add(event.streamId);
                            const err = new Set(context.erroredStreamIds);
                            err.delete(event.streamId);
                            return {
                                activeStreamIds: active,
                                erroredStreamIds: err,
                                transportDisconnectedByAllStreamsFail: false,
                            };
                        }),
                        ({ context, event }) => {
                            if (event.reconnectTransport) {
                                context.onTransportConnect();
                            }
                        },
                    ],
                },
                STREAM_ERROR: {
                    actions: [
                        assign(({ context, event }) => {
                            const err = new Set(context.erroredStreamIds);
                            err.add(event.streamId);
                            const allFailed =
                                context.activeStreamIds.size > 0 &&
                                err.size === context.activeStreamIds.size;
                            return {
                                erroredStreamIds: err,
                                transportDisconnectedByAllStreamsFail:
                                    context.transportDisconnectedByAllStreamsFail || allFailed,
                            };
                        }),
                        ({ context }) => {
                            if (
                                context.activeStreamIds.size > 0 &&
                                context.erroredStreamIds.size === context.activeStreamIds.size
                            ) {
                                context.onTransportDisconnect();
                            }
                        },
                    ],
                },
            },
        },
    },
});
