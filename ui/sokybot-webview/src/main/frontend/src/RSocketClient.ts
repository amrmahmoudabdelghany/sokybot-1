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

export class RSocketService {
    private client: any;

    connect(): Promise<void> {
        return new Promise((resolve, reject) => {
            const client = new RSocketClient({
                serializers: {
                    data: IdentitySerializer,
                    metadata: IdentitySerializer,
                },
                setup: {
                    keepAlive: 60000,
                    lifetime: 180000,
                    dataMimeType: 'application/json',
                    metadataMimeType: 'text/plain',
                },
                transport: new RSocketWebSocketClient({
                    url: getRSocketUrl(),
                    wsCreator: (url: string) => new WebSocket(url),
                }),
            });

            client.connect().subscribe({
                onComplete: (socket: any) => {
                    this.client = socket;
                    console.log('Connected to RSocket');
                    resolve();
                },
                onError: (error: any) => {
                    console.error('Connection failed', error);
                    reject(error);
                },
                onSubscribe: (_cancel: any) => { }
            });
        });
    }

    /**
     * Send a request using the new structured protocol.
     * 
     * @param method - The method name (e.g., "character.state", "machine.list")
     * @param params - Optional parameters object
     * @returns Promise resolving to the response result
     */
    async request<T = unknown>(method: string, params?: Record<string, unknown>): Promise<T> {
        if (!this.client) {
            throw new Error("Client not connected");
        }

        const request: RSocketRequest = {
            method,
            params,
            id: generateRequestId(),
        };

        return new Promise((resolve, reject) => {
            const payload = {
                data: JSON.stringify(request),
                metadata: ""
            };

            this.client.requestResponse(payload).subscribe({
                onComplete: (response: any) => {
                    try {
                        const data: RSocketResponse<T> = typeof response.data === 'string'
                            ? JSON.parse(response.data)
                            : response.data;

                        if (data.error) {
                            reject(new RSocketError(data.error.code, data.error.message, data.error.data));
                        } else {
                            resolve(data.result as T);
                        }
                    } catch (e) {
                        reject(e);
                    }
                },
                onError: (error: any) => reject(error)
            });
        });
    }

    /**
     * Legacy request-response method for backwards compatibility.
     * @deprecated Use request() instead
     */
    requestResponse(message: string): Promise<string> {
        if (!this.client) {
            return Promise.reject("Client not connected");
        }
        return new Promise((resolve, reject) => {
            const payload = {
                data: message,
                metadata: ""
            };

            this.client.requestResponse(payload).subscribe({
                onComplete: (response: any) => {
                    resolve(response.data);
                },
                onError: (error: any) => reject(error)
            });
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
            console.error("Client not connected");
            return { unsubscribe: () => { }, request: () => { } };
        }

        const normalizedOptions: SubscribeOptions =
            options && !('params' in options) && !('requestN' in options) && !('initialRequestN' in options) && !('maxInFlight' in options)
                ? { params: options as Record<string, unknown> }
                : (options as SubscribeOptions || {});
        const params = normalizedOptions.params;

        const request: RSocketRequest = {
            method,
            params,
            id: generateRequestId(),
        };

        const payload = {
            data: JSON.stringify(request),
            metadata: ""
        };

        let subscription: any;
        let inFlight = 0;
        const initialRequestN = Math.max(1, normalizedOptions.initialRequestN ?? normalizedOptions.requestN ?? 64);
        const requestN = Math.max(1, normalizedOptions.requestN ?? initialRequestN);
        const maxInFlight = Math.max(requestN, normalizedOptions.maxInFlight ?? requestN * 4);

        this.client.requestStream(payload).subscribe({
            onNext: (payload: any) => {
                inFlight = Math.max(0, inFlight - 1);
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

                    if (subscription && inFlight < requestN) {
                        const delta = Math.min(requestN, maxInFlight - inFlight);
                        if (delta > 0) {
                            subscription.request(delta);
                            inFlight += delta;
                        }
                    }
                } catch (e) {
                    console.error("Failed to parse stream data", e);
                }
            },
            onError: (error: any) => {
                if (onError) {
                    onError(error);
                } else {
                    console.error("Stream error", error);
                }
            },
            onSubscribe: (sub: any) => {
                subscription = sub;
                subscription.request(initialRequestN);
                inFlight = initialRequestN;
            }
        });

        return {
            unsubscribe: () => {
                if (subscription) {
                    subscription.cancel();
                }
            },
            request: (n: number) => {
                const count = Math.max(1, Math.floor(n || 1));
                if (subscription) {
                    subscription.request(count);
                    inFlight += count;
                }
            }
        };
    }

    /**
     * Legacy stream events method for backwards compatibility.
     * @deprecated Use subscribe() instead
     */
    streamEvents(onEvent: (event: any) => void, onError: (error: any) => void) {
        return this.subscribe("game.events", onEvent, onError);
    }

    /**
     * Legacy request stream method for backwards compatibility.
     * @deprecated Use subscribe() instead
     */
    requestStream(
        message: string,
        onNext: (data: any) => void,
        onError?: (error: any) => void
    ): RSocketSubscription {
        if (!this.client) {
            console.error("Client not connected");
            return { unsubscribe: () => { }, request: () => { } };
        }

        const payload = {
            data: message,
            metadata: ""
        };

        let subscription: any;

        this.client.requestStream(payload).subscribe({
            onNext: (payload: any) => {
                try {
                    const data = typeof payload.data === 'string'
                        ? JSON.parse(payload.data)
                        : payload.data;
                    onNext(data);
                } catch (e) {
                    console.error("Failed to parse stream data", e);
                }
            },
            onError: (error: any) => {
                if (onError) {
                    onError(error);
                } else {
                    console.error("Stream error", error);
                }
            },
            onSubscribe: (sub: any) => {
                subscription = sub;
                subscription.request(2147483647);
            }
        });

        return {
            unsubscribe: () => {
                if (subscription) {
                    subscription.cancel();
                }
            },
            request: (n: number) => {
                if (subscription) {
                    subscription.request(Math.max(1, Math.floor(n || 1)));
                }
            }
        };
    }

    fireAndForget(method: string, params?: Record<string, unknown>): Promise<void> {
        if (!this.client) {
            return Promise.reject(new Error("Client not connected"));
        }
        const request: RSocketRequest = {
            method,
            params,
            id: generateRequestId(),
        };
        const payload = {
            data: JSON.stringify(request),
            metadata: ""
        };
        return this.client.fireAndForget(payload);
    }

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

        const source = {
            subscribe: (subscriber: any) => {
                if (subscriber.onSubscribe) {
                    subscriber.onSubscribe({
                        request: (_n: number) => {
                            // Eagerly emits buffered items when requested.
                            sourceItems.forEach((item) => {
                                subscriber.onNext({
                                    data: JSON.stringify({
                                        method,
                                        params: { ...params, item },
                                        id: generateRequestId(),
                                    }),
                                    metadata: ""
                                });
                            });
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
     * Legacy request stream with state for backwards compatibility.
     * @deprecated Use subscribe() instead
     */
    requestStreamWithState(
        message: string,
        stateKey: string,
        onStateUpdate: (state: Record<string, any>) => void,
        onError?: (error: any) => void
    ): { unsubscribe: () => void } {
        return this.requestStream(
            message,
            (data) => {
                onStateUpdate({ [stateKey]: data });
            },
            onError
        );
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
    async createGroup(name: string, path: string) {
        return this.request<{ status: string; name: string }>('group.create', { name, path });
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
    inGame?: boolean;
    agentOptions?: Array<{ value: string; label: string }>;
    availableCharacters?: string[];
    selectedCharacter?: string | null;
    savedLogin?: SavedLoginSnapshot;
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
}

export interface GroupDetails {
    name: string;
    version: string;
    hosts: Record<string, string[]>;
    machineCount: number;
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
    machineId: string;
    transition?: string;
    connected?: boolean;
    authenticated?: boolean;
    loginPhase?: string;
    timestamp?: number;
    reason?: string;
    host?: string;
    port?: number;
    topic?: string;
}

export const rsocketService = new RSocketService();
