import { RSocketClient, IdentitySerializer } from 'rsocket-core';
import RSocketWebSocketClient from 'rsocket-websocket-client';

/**
 * RSocket request format following JSON-RPC-like protocol.
 */
export interface RSocketRequest {
    method: string;
    params?: Record<string, unknown>;
    id?: string;
}

/**
 * RSocket response format.
 */
export interface RSocketResponse<T = unknown> {
    result?: T;
    error?: {
        code: number;
        message: string;
        data?: unknown;
    };
    id?: string;
}

export interface SubscribeOptions {
    params?: Record<string, unknown>;
    initialRequestN?: number;
    requestN?: number;
    maxInFlight?: number;
}

export interface RSocketSubscription {
    unsubscribe: () => void;
    request: (n: number) => void;
}

/**
 * Standard error codes (following JSON-RPC conventions).
 */
export const ErrorCode = {
    PARSE_ERROR: -32700,
    INVALID_REQUEST: -32600,
    METHOD_NOT_FOUND: -32601,
    INVALID_PARAMS: -32602,
    INTERNAL_ERROR: -32603,
    NOT_FOUND: 404,
    UNAUTHORIZED: 401,
    FORBIDDEN: 403,
    SERVICE_UNAVAILABLE: 503,
} as const;

/** Keep aligned with {@link org.sokybot.webview.WebviewProtocolConstants#PROTOCOL_API_VERSION}. */
export const SOKYBOT_PROTOCOL_API_VERSION = 1;

function getUiBuildId(): string {
    const id = import.meta.env.VITE_UI_BUILD_ID;
    return typeof id === 'string' && id.length > 0 ? id : 'dev';
}

function delay(ms: number): Promise<void> {
    return new Promise((resolve) => window.setTimeout(resolve, ms));
}

const getRSocketUrl = () => {
    // Check for environment variable override
    const envUrl = (import.meta as any).env.VITE_BACKEND_URL;
    if (envUrl) {
        console.log('Using RSocket backend URL from environment:', envUrl);
        return envUrl;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.hostname;
    // In development, Vite runs on 5173 but backend is on 8080
    const port = window.location.port === '5173' ? '8080' : window.location.port;
    return `${protocol}//${host}:${port}/rsocket`;
};

let requestIdCounter = 0;

function generateRequestId(): string {
    return `req-${++requestIdCounter}-${Date.now()}`;
}

export type ConnectionState = 'connecting' | 'connected' | 'disconnected';

export class TimeoutError extends Error {
    constructor(message: string) {
        super(message);
        this.name = 'TimeoutError';
    }
}

type QueuedRequest<T = unknown> = {
    key: string;
    method: string;
    params?: Record<string, unknown>;
    expiresAt: number;
    resolve: (value: T) => void;
    reject: (reason?: unknown) => void;
};

export class RSocketService {
    private client: any;
    private state: ConnectionState = 'disconnected';
    private readonly stateListeners = new Set<(state: ConnectionState) => void>();
    private readonly reconnectListeners = new Set<() => void>();
    private reconnectTimer: number | undefined;
    private reconnectAttempts = 0;
    private connectingPromise: Promise<void> | null = null;
    private isManualClose = false;
    private readonly queue: QueuedRequest<unknown>[] = [];
    private readonly requestTtlMs = 5000;
    private readonly maxQueueSize = 200;
    private readonly queueableMethodPrefixes = [
        'machine.', 'group.', 'workspace.', 'extension.', 'fs.', 'system.', 'character.',
    ];
    private readonly activeStreams = new Map<string, () => void>();
    private readonly beforeUnloadHandler = () => this.close();
    private workspaceBootstrapMode: 'summary' | 'legacy' = 'summary';

    connect(): Promise<void> {
        if (this.connectingPromise) return this.connectingPromise;
        this.isManualClose = false;
        this.setState('connecting');
        if (typeof window !== 'undefined') {
            window.removeEventListener('beforeunload', this.beforeUnloadHandler);
            window.addEventListener('beforeunload', this.beforeUnloadHandler);
        }
        this.connectingPromise = new Promise((resolve, reject) => {
            const client = new RSocketClient({
                serializers: { data: IdentitySerializer, metadata: IdentitySerializer },
                setup: {
                    keepAlive: 60000,
                    lifetime: 180000,
                    dataMimeType: 'application/json',
                    metadataMimeType: 'text/plain',
                    payload: {
                        data: JSON.stringify({
                            minProtocolApi: SOKYBOT_PROTOCOL_API_VERSION,
                            uiBuildId: getUiBuildId(),
                        }),
                        metadata: '',
                    },
                },
                transport: new RSocketWebSocketClient({
                    url: getRSocketUrl(),
                    wsCreator: (url: string) => new WebSocket(url),
                }),
            });
            client.connect().subscribe({
                onComplete: (socket: any) => {
                    this.client = socket;
                    this.reconnectAttempts = 0;
                    this.setState('connected');
                    this.connectingPromise = null;
                    this.notifyReconnected();
                    this.flushQueue();
                    resolve();
                },
                onError: (error: any) => {
                    this.connectingPromise = null;
                    this.client = null;
                    this.setState('disconnected');
                    this.scheduleReconnect();
                    reject(error);
                },
                onSubscribe: (_cancel: any) => { }
            });
        });
        return this.connectingPromise;
    }

    /**
     * Send a request using the new structured protocol.
     * 
     * @param method - The method name (e.g., "character.state", "machine.list")
     * @param params - Optional parameters object
     * @returns Promise resolving to the response result
     */
    async request<T = unknown>(method: string, params?: Record<string, unknown>): Promise<T> {
        return new Promise<T>((resolve, reject) => {
            if (!this.client) {
                if (!this.shouldQueue(method)) {
                    reject(new Error('Client not connected'));
                    return;
                }
                this.enqueueRequest({ method, params, resolve: (value) => resolve(value as T), reject });
                this.ensureConnected();
                return;
            }
            this.dispatchRequest({ method, params, resolve: (value) => resolve(value as T), reject });
        });
    }

    /**
     * Subscribe to a stream using the new structured protocol.
     * 
     * @param method - The stream method name (e.g., "game.events")
     * @param onNext - Callback for each stream item
     * @param onError - Optional error callback
     * @param params - Optional parameters
     */
    subscribe<T = unknown>(
        method: string,
        onNext: (data: T) => void,
        onError?: (error: any) => void,
        options?: Record<string, unknown> | SubscribeOptions
    ): RSocketSubscription {
        if (!this.client) {
            this.ensureConnected();
        }

        const normalizedOptions: SubscribeOptions =
            options && !('params' in options) && !('requestN' in options) && !('initialRequestN' in options) && !('maxInFlight' in options)
                ? { params: options as Record<string, unknown> }
                : (options as SubscribeOptions || {});
        const params = normalizedOptions.params;

        const id = generateRequestId();
        const request: RSocketRequest = { method, params, id };
        const payload = this.makePayloadEnvelope(method, request);

        let subscription: any;
        const initialRequestN = Math.max(1, normalizedOptions.initialRequestN ?? normalizedOptions.requestN ?? 64);

        const subscribeNow = () => this.client?.requestStream(payload).subscribe({
            onNext: (payload: any) => {
                try {
                    const data = typeof payload.data === 'string'
                        ? JSON.parse(payload.data)
                        : payload.data;

                    // Check if it's an error response
                    if (data && data.error) {
                        if (onError) {
                            onError(new RSocketError(data.error.code, data.error.message, data.error.data));
                        }
                    } else {
                        onNext(data as T);
                    }
                } catch (e) {
                    console.error("Failed to parse stream data", e);
                } finally {
                    // RSocket request-stream: replenish demand per delivered frame. The previous
                    // inFlight/requestN heuristic could stop calling request() while credits were
                    // exhausted, stalling high-volume streams (e.g. Traffic Monitor) until reconnect.
                    if (subscription) {
                        subscription.request(1);
                    }
                }
            },
            onError: (error: any) => {
                this.handleTransportFailure(error);
                if (onError) {
                    onError(error);
                } else {
                    console.error("Stream error", error);
                }
            },
            onSubscribe: (sub: any) => {
                subscription = sub;
                subscription.request(initialRequestN);
            }
        });

        const streamKey = `${method}:${JSON.stringify(params ?? {})}:${generateRequestId()}`;
        this.activeStreams.set(streamKey, subscribeNow);
        if (this.client) subscribeNow();
        else this.ensureConnected();

        return {
            unsubscribe: () => {
                this.activeStreams.delete(streamKey);
                if (subscription) {
                    subscription.cancel();
                }
            },
            request: (n: number) => {
                const count = Math.max(1, Math.floor(n || 1));
                if (subscription) {
                    subscription.request(count);
                }
            }
        };
    }

    fireAndForget(method: string, params?: Record<string, unknown>): Promise<void> {
        if (!this.client) {
            if (!this.shouldQueue(method)) {
                return Promise.reject(new Error('Client not connected'));
            }
            return new Promise<void>((resolve, reject) => {
                this.enqueueRequest({ method, params, resolve: () => resolve(), reject });
                this.ensureConnected();
            });
        }
        const request: RSocketRequest = { method, params, id: generateRequestId() };
        const payload = this.makePayloadEnvelope(method, request);
        // rsocket-core RSocketClient.fireAndForget() returns void, not a Promise — do not chain .catch().
        try {
            this.client.fireAndForget(payload);
            return Promise.resolve();
        } catch (e: unknown) {
            this.handleTransportFailure(e);
            return Promise.reject(e);
        }
    }

    /**
     * UTF-8 route in metadata (dual-read with JSON {@code method} on the server).
     */
    private makePayloadEnvelope(method: string, body: RSocketRequest): { data: string; metadata: string } {
        return {
            data: JSON.stringify(body),
            metadata: method,
        };
    }

    /**
     * Legacy one-shot channel: emits a fixed list of outbound frames then completes.
     */
    requestChannel<TOut = unknown, TIn = unknown>(
        method: string,
        sourceItems: TOut[],
        onNext: (data: TIn) => void,
        onError?: (error: any) => void,
        params?: Record<string, unknown>
    ): RSocketSubscription {
        if (!this.client) {
            console.error("Client not connected");
            return { unsubscribe: () => { }, request: () => { } };
        }

        const frames = sourceItems.map((item) =>
            this.makePayloadEnvelope(method, {
                method,
                params: { ...params, item },
                id: generateRequestId(),
            })
        );

        const source = {
            subscribe: (subscriber: any) => {
                if (subscriber.onSubscribe) {
                    subscriber.onSubscribe({
                        request: (_n: number) => {
                            frames.forEach((pl) => subscriber.onNext(pl));
                            subscriber.onComplete?.();
                        },
                        cancel: () => { }
                    });
                }
            }
        };

        let subscription: any;
        this.client.requestChannel(source).subscribe({
            onNext: (payload: any) => {
                try {
                    const data = typeof payload.data === 'string'
                        ? JSON.parse(payload.data)
                        : payload.data;
                    onNext(data as TIn);
                } catch (e) {
                    console.error("Failed to parse channel data", e);
                }
            },
            onError: (error: any) => onError?.(error),
            onSubscribe: (sub: any) => {
                subscription = sub;
                subscription.request(64);
            }
        });

        return {
            unsubscribe: () => subscription?.cancel?.(),
            request: (n: number) => subscription?.request?.(Math.max(1, Math.floor(n || 1)))
        };
    }

    /**
     * Bidirectional channel: outbound frames are queued and delivered under RSocket demand.
     * Initial params are sent as the first frame after subscribe.
     */
    openInteractiveChannel(options: {
        method: string;
        initialParams?: Record<string, unknown>;
        initialRequestN?: number;
        onMessage: (data: unknown) => void;
        onError?: (error: unknown) => void;
    }): {
        send: (params: Record<string, unknown>) => void;
        close: () => void;
        request: (n: number) => void;
    } {
        const method = options.method;
        const queue: Array<{ data: string; metadata: string }> = [];
        let requested = 0;
        let cancelled = false;
        let outboundSubscriber: any = null;
        let downstreamSubscription: any = null;

        const push = (params: Record<string, unknown>) => {
            if (cancelled) return;
            queue.push(
                this.makePayloadEnvelope(method, {
                    method,
                    params,
                    id: generateRequestId(),
                })
            );
            drain();
        };

        const drain = () => {
            while (!cancelled && outboundSubscriber && requested > 0 && queue.length > 0) {
                const item = queue.shift()!;
                outboundSubscriber.onNext(item);
                requested--;
            }
        };

        const source = {
            subscribe: (subscriber: any) => {
                outboundSubscriber = subscriber;
                subscriber.onSubscribe({
                    request: (n: number) => {
                        requested += Math.max(1, Math.floor(n || 1));
                        drain();
                    },
                    cancel: () => {
                        cancelled = true;
                        queue.length = 0;
                        outboundSubscriber = null;
                    },
                });
                push(options.initialParams ?? {});
            },
        };

        const start = () => {
            if (!this.client) return;
            this.client.requestChannel(source).subscribe({
                onNext: (payload: any) => {
                    try {
                        const data = typeof payload.data === 'string'
                            ? JSON.parse(payload.data)
                            : payload.data;
                        if (data && data.error) {
                            options.onError?.(new RSocketError(data.error.code, data.error.message, data.error.data));
                        } else {
                            options.onMessage(data);
                        }
                    } catch (e) {
                        console.error('Failed to parse interactive channel data', e);
                    }
                },
                onError: (error: any) => {
                    this.handleTransportFailure(error);
                    options.onError?.(error);
                },
                onSubscribe: (sub: any) => {
                    downstreamSubscription = sub;
                    sub.request(Math.max(1, options.initialRequestN ?? 64));
                },
            });
        };

        if (!this.client) {
            void this.connect()
                .then(start)
                .catch((e) => options.onError?.(e));
        } else {
            start();
        }

        return {
            send: (params) => push(params),
            close: () => {
                cancelled = true;
                queue.length = 0;
                downstreamSubscription?.cancel?.();
            },
            request: (n: number) =>
                downstreamSubscription?.request?.(Math.max(1, Math.floor(n || 1))),
        };
    }

    onConnectionStateChange(listener: (state: ConnectionState) => void): () => void {
        this.stateListeners.add(listener);
        listener(this.state);
        return () => this.stateListeners.delete(listener);
    }

    onReconnect(listener: () => void): () => void {
        this.reconnectListeners.add(listener);
        return () => this.reconnectListeners.delete(listener);
    }

    getConnectionState(): ConnectionState {
        return this.state;
    }

    isClosed(): boolean {
        return this.state === 'disconnected' && !this.client;
    }

    close(): void {
        this.isManualClose = true;
        if (this.reconnectTimer) {
            window.clearTimeout(this.reconnectTimer);
            this.reconnectTimer = undefined;
        }
        if (this.client?.close) {
            try { this.client.close(); } catch { }
        }
        this.client = null;
        this.rejectQueue(new Error('Connection closed'));
        this.setState('disconnected');
        if (typeof window !== 'undefined') {
            window.removeEventListener('beforeunload', this.beforeUnloadHandler);
        }
    }

    private dispatchRequest(q: Omit<QueuedRequest<unknown>, 'expiresAt' | 'key'>): void {
        const id = generateRequestId();
        const request: RSocketRequest = { method: q.method, params: q.params, id };
        const payload = this.makePayloadEnvelope(q.method, request);
        this.client.requestResponse(payload).subscribe({
            onComplete: (response: any) => {
                try {
                    const data: RSocketResponse<unknown> = typeof response.data === 'string' ? JSON.parse(response.data) : response.data;
                    if (data.error) q.reject(new RSocketError(data.error.code, data.error.message, data.error.data));
                    else q.resolve(data.result);
                } catch (e) { q.reject(e); }
            },
            onError: (error: any) => {
                this.handleTransportFailure(error);
                q.reject(error);
            }
        });
    }

    private shouldQueue(method: string): boolean {
        return this.queueableMethodPrefixes.some((prefix) => method.startsWith(prefix));
    }

    private enqueueRequest(req: Omit<QueuedRequest<unknown>, 'expiresAt' | 'key'>): void {
        this.dropExpired();
        if (this.queue.length >= this.maxQueueSize) {
            req.reject(new TimeoutError('Request queue is full'));
            return;
        }
        const entry: QueuedRequest<unknown> = {
            ...req,
            key: '',
            expiresAt: Date.now() + this.requestTtlMs
        };
        this.coalesce(entry);
        this.queue.push(entry);
    }

    private coalesce(entry: QueuedRequest): void {
        const key = this.queueKey(entry.method, entry.params);
        entry.key = key;
        if (entry.method === 'machine.start' || entry.method === 'machine.stop') {
            for (let i = this.queue.length - 1; i >= 0; i--) {
                if (this.queue[i].key === key) {
                    const replaced = this.queue.splice(i, 1)[0];
                    replaced.reject(new TimeoutError('Superseded by newer command'));
                }
            }
        }
    }

    private queueKey(method: string, params?: Record<string, unknown>): string {
        const machineId = typeof params?.machineId === 'string' ? params.machineId : '';
        return `${method}:${machineId}`;
    }

    private flushQueue(): void {
        this.dropExpired();
        if (!this.client) return;
        const queued = this.queue.splice(0, this.queue.length);
        queued.forEach((req) => this.dispatchRequest(req));
    }

    private dropExpired(): void {
        const now = Date.now();
        for (let i = this.queue.length - 1; i >= 0; i--) {
            if (this.queue[i].expiresAt <= now) {
                const expired = this.queue.splice(i, 1)[0];
                expired.reject(new TimeoutError('Queued request expired'));
            }
        }
    }

    private rejectQueue(error: Error): void {
        const queued = this.queue.splice(0, this.queue.length);
        queued.forEach((item) => item.reject(error));
    }

    private setState(next: ConnectionState): void {
        if (this.state === next) return;
        this.state = next;
        this.stateListeners.forEach((listener) => listener(next));
    }

    private ensureConnected(): void {
        if (!this.connectingPromise && !this.client && !this.isManualClose) {
            this.connect().catch(() => { /* retried by scheduleReconnect */ });
        }
    }

    private scheduleReconnect(): void {
        if (this.isManualClose || this.reconnectTimer) return;
        const base = Math.min(30000, 1000 * (2 ** this.reconnectAttempts));
        const jitter = base * 0.2 * ((Math.random() * 2) - 1);
        const delay = Math.max(250, Math.floor(base + jitter));
        this.reconnectAttempts += 1;
        this.reconnectTimer = window.setTimeout(() => {
            this.reconnectTimer = undefined;
            this.ensureConnected();
        }, delay);
    }

    private handleTransportFailure(_error: unknown): void {
        if (this.isManualClose) return;
        this.client = null;
        this.setState('disconnected');
        this.scheduleReconnect();
    }

    private notifyReconnected(): void {
        this.workspaceBootstrapMode = 'summary';
        this.reconnectListeners.forEach((listener) => listener());
        const entries = Array.from(this.activeStreams.entries());
        this.activeStreams.clear();
        entries.forEach(([key, resubscribe]) => {
            this.activeStreams.set(key, resubscribe);
            resubscribe();
        });
    }

    // ============ Convenience Methods ============

    /**
     * Get character state for a machine.
     */
    async getCharacterState(machineId?: string) {
        return this.request<CharacterState>('character.state', machineId ? { machineId } : undefined);
    }

    /**
     * Start a bot machine.
     */
    async startBot(machineId: string) {
        return this.request<{ status: string; machineId: string }>('machine.start', { machineId });
    }

    /**
     * Stop a bot machine.
     */
    async stopBot(machineId: string) {
        return this.request<{ status: string; machineId: string }>('machine.stop', { machineId });
    }

    /**
     * List all machines.
     */
    async getMachines() {
        return this.request<MachineInfo[]>('machine.list');
    }

    /**
     * How the last successful workspace bootstrap was loaded (for UX / diagnostics).
     */
    getWorkspaceBootstrapMode(): 'summary' | 'legacy' {
        return this.workspaceBootstrapMode;
    }

    /**
     * Single-call bootstrap: groups + machines (preferred over separate list calls).
     * Retries {@code METHOD_NOT_FOUND} during the startup window (Karaf SCR wiring), then falls back
     * to {@code group.list} + {@code machine.list} on older backends.
     */
    async getWorkspaceSummary(): Promise<WorkspaceSummary> {
        const deadline = Date.now() + 5000;
        let lastMethodNotFound: RSocketError | null = null;
        let attempt = 0;
        while (Date.now() < deadline) {
            try {
                const r = await this.request<WorkspaceSummary>('workspace.summary');
                this.workspaceBootstrapMode = 'summary';
                return r;
            } catch (e) {
                if (e instanceof RSocketError && e.code === ErrorCode.METHOD_NOT_FOUND) {
                    lastMethodNotFound = e;
                    attempt += 1;
                    await delay(Math.min(600, 80 + attempt * 120));
                    continue;
                }
                throw e;
            }
        }
        try {
            const [groups, machines] = await Promise.all([this.getGroups(), this.getMachines()]);
            this.workspaceBootstrapMode = 'legacy';
            console.debug(
                'workspace.summary unavailable after startup window; using group.list + machine.list'
            );
            return { groups, machines };
        } catch (e) {
            if (lastMethodNotFound) {
                throw lastMethodNotFound;
            }
            throw e;
        }
    }

    /**
     * Create a new machine.
     */
    async createMachine(group: string, name: string, options?: string[]) {
        return this.request<{ status: string; machineId: string }>('machine.create', { group, name, options });
    }

    /**
     * Initialize a machine with dynamic target settings (like gateway, credentials).
     */
    async initializeMachine(group: string, name: string, scope: string, payload: Record<string, unknown>) {
        return this.request<{ status: string; machineId: string }>('machine.initialize', { group, name, scope, payload });
    }

    /**
     * List all groups.
     */
    async getGroups() {
        return this.request<GroupInfo[]>('group.list');
    }

    /**
     * Get group details.
     */
    async getGroupDetails(name: string) {
        return this.request<GroupDetails>('group.details', { name });
    }

    /**
     * Create a new group.
     */
    async createGroup(name: string, path: string, isManualOverride?: boolean, manualHost?: string, manualDivision?: string) {
        return this.request<{ status: string; name: string }>('group.create', {
            name,
            path,
            isManualOverride,
            manualHost,
            manualDivision
        });
    }

    /**
     * List files in a directory.
     */
    async listFiles(path?: string) {
        return this.request<FileListResult>('fs.list', path ? { path } : undefined);
    }

    /**
     * Get filesystem roots.
     */
    async getFileRoots() {
        return this.request<FileInfo[]>('fs.roots');
    }

    /**
     * Get extension registry.
     */
    async getExtensionRegistry() {
        return this.request<ExtensionRegistry>('extension.registry');
    }

    /**
     * Get extension page schema.
     */
    async getExtensionSchema(pageId: string, machineId?: string) {
        return this.request<{ schema: Record<string, unknown> }>('extension.schema', { pageId, machineId });
    }

    /**
     * Trigger an extension action.
     */
    async triggerExtensionAction(pageId: string, action: string, data?: Record<string, unknown>) {
        return this.request<Record<string, unknown>>('extension.action', { pageId, action, data });
    }

    /**
     * Trigger a toolbar action.
     */
    async triggerToolbarAction(actionId: string, action: string, data?: Record<string, unknown>) {
        return this.request<Record<string, unknown>>('extension.toolbar.action', { actionId, action, data });
    }

    /**
     * Get system information.
     */
    async getSystemInfo() {
        return this.request<SystemInfo>('system.info');
    }

    /**
     * Get available methods.
     */
    async getMethods() {
        return this.request<{ methods: Record<string, string> }>('system.methods');
    }

    /**
     * Subscribe to game events.
     */
    subscribeToGameEvents(
        onEvent: (event: GameEvent) => void,
        onError?: (error: any) => void
    ) {
        return this.subscribe<GameEvent>('game.events', onEvent, onError);
    }

    /**
     * Subscribe to extension events.
     */
    subscribeToExtensionEvents(
        onEvent: (event: ExtensionEvent) => void,
        onError?: (error: any) => void
    ) {
        return this.subscribe<ExtensionEvent>('extension.events', onEvent, onError);
    }

    subscribeToMachineStatus(
        machineId: string,
        onEvent: (event: MachineStatusEvent) => void,
        onError?: (error: any) => void
    ) {
        return this.subscribe<MachineStatusEvent>(
            'machine.status.stream',
            onEvent,
            onError,
            { machineId }
        );
    }

    /**
     * Best-effort: notifies backend that the web UI session is active (RSocket fire-and-forget).
     */
    notifyClientConnected(): Promise<void> {
        return this.fireAndForget('webview.client.connected', { uiBuildId: getUiBuildId() });
    }

    /**
     * Open the diagnostics request-channel (filtered {@code IEventBridge} stream + commands).
     */
    openDiagnosticsChannel(options: {
        pattern?: string;
        machineId?: string;
        initialRequestN?: number;
        onMessage: (data: unknown) => void;
        onError?: (error: unknown) => void;
    }) {
        return this.openInteractiveChannel({
            method: 'diagnostics.stream',
            initialParams: {
                pattern: options.pattern ?? 'sokybot/**',
                ...(options.machineId ? { machineId: options.machineId } : {}),
            },
            initialRequestN: options.initialRequestN,
            onMessage: options.onMessage,
            onError: options.onError,
        });
    }
}

/**
 * Custom error class for RSocket errors.
 */
export class RSocketError extends Error {
    code: number;
    data?: unknown;

    constructor(code: number, message: string, data?: unknown) {
        super(message);
        this.name = 'RSocketError';
        this.code = code;
        this.data = data;
    }
}

// ============ Type Definitions ============

/** Non-secret persisted login fields from settings (for sidebar hydration). */
export interface SavedLoginSnapshot {
    targetGateway?: string;
    targetAgent?: string;
    username?: string;
    password?: string;
    passcode?: string;
    selectedCharacter?: string;
    selectedCharacterSlot?: number;
    characterSlotBase?: number;
    characterSelectionStrictMode?: boolean;
    agentWaitTimeoutMs?: number;
    loginResponseTimeoutMs?: number;
    agentAuthTimeoutMs?: number;
    passcodeWaitTimeoutMs?: number;
    passcodeUserInputTimeoutMs?: number;
    usernameSet?: boolean;
    autoLogin?: boolean;
    autoReconnect?: boolean;
}

export interface CharacterState {
    entityId: number;
    characterName: string;
    level: number;
    currentHP: number;
    maxHP: number;
    currentMP: number;
    maxMP: number;
    gold: number;
    xSector: number;
    x: number;
    y: number;
    isRunning: boolean;
    connected?: boolean;
    loginPhase?: string;
    agentsDiscovered?: number;
    authenticated?: boolean;
    /** True after agent auth success on the model (may be true during character selection). */
    signInComplete?: boolean;
    /** True when the engine is waiting for character pick (step 4). */
    awaitingCharacterSelection?: boolean;
    inGame?: boolean;
    agentOptions?: Array<{ value: string; label: string }>;
    availableCharacters?: string[];
    selectedCharacter?: string | null;
    savedLogin?: SavedLoginSnapshot;
    gatewayResultCode?: number | null;
    agentAuthResultCode?: number | null;
    failureReason?: string | null;
    loginDetailMessage?: string | null;
    queuePosition?: number | null;
}

export interface MachineInfo {
    machineId: string;
    name: string;
    groupName?: string;
    isRunning: boolean;
}

export interface GroupInfo {
    name: string;
    machineCount?: number;
    isManualOverride?: boolean;
    manualHost?: string;
    manualDivision?: string;
}

export interface WorkspaceSummary {
    groups: GroupInfo[];
    machines: MachineInfo[];
}

export interface GroupDetails {
    name: string;
    version: string;
    port: number;
    hosts: Record<string, string[]>;
    machineCount: number;
    isManualOverride?: boolean;
    manualHost?: string;
    manualDivision?: string;
}

export interface FileInfo {
    name: string;
    path: string;
    isDirectory: boolean;
}

export interface FileListResult {
    current: string;
    files: FileInfo[];
}

export interface ExtensionRegistry {
    pages: Record<string, {
        pageId: string;
        title: string;
        iconPath?: string;
        schema: Record<string, unknown>;
    }>;
    toolbarActions: Record<string, {
        actionId: string;
        title: string;
        iconName: string;
        modalSchema?: Record<string, unknown>;
    }>;
}

export interface SystemInfo {
    version: string;
    protocol: string;
    protocolVersion: string;
    /** Integer API level; aligns with {@link SOKYBOT_PROTOCOL_API_VERSION}. */
    protocolApi?: number;
    /** OSGi bundle version for sokybot-webview (diagnostics). */
    webviewBundleVersion?: string;
    memory: {
        free: number;
        total: number;
        max: number;
    };
    processors: number;
    javaVersion: string;
    osName: string;
}

export interface GameEvent {
    [key: string]: unknown;
}

export interface ExtensionEvent {
    type: string;
    data: Record<string, unknown>;
    timestamp: number;
}

export interface MachineStatusEvent {
    /** e.g. heartbeat, MACHINE_REMOVED */
    type?: string;
    machineId: string;
    transition?: string;
    connected?: boolean;
    authenticated?: boolean;
    loginPhase?: string;
    inGame?: boolean;
    agentOptions?: Array<{ value: string; label: string }>;
    availableCharacters?: string[];
    selectedCharacter?: string | null;
    timestamp?: number;
    reason?: string;
    host?: string;
    port?: number;
    topic?: string;
    /** Retry delay in milliseconds (present when phase is RETRY_DELAY). */
    retryDelayMs?: number;
    /** Server-side timestamp when the retry was scheduled. */
    serverTimestamp?: number;
    /** Absolute UTC ms when retry will fire (preferred over retryDelayMs+serverTimestamp). */
    retryAt?: number;
    /** Backend failure classification (NETWORK, CREDENTIAL, AGENT_TIMEOUT, MANUAL_VERIFICATION). */
    failureClass?: string;
    /** True when the failure is non-retryable (credential rejection, manual verification, etc.). */
    fatal?: boolean;
    /** Backend UX category hint for onboarding rendering. */
    uxCategory?: 'CONNECT' | 'AGENT' | 'AUTH' | 'CHARACTER' | 'INGAME' | 'ERROR';
    /** Backend UX hint indicating user action/challenge is required. */
    requiresInput?: boolean;
    /** Latest measured transport latency from heartbeat RTT, in milliseconds. */
    latencyMs?: number;
    loginDetailMessage?: string | null;
    gatewayResultCode?: number | null;
    agentAuthResultCode?: number | null;
    failureReason?: string | null;
    queuePosition?: number | null;
    signInComplete?: boolean;
    awaitingCharacterSelection?: boolean;
}

export const createRSocketService = () => new RSocketService();
export const rsocketService = createRSocketService();
