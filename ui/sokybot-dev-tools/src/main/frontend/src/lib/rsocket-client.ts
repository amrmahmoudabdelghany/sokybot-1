import { RSocketClient, IdentitySerializer } from 'rsocket-core';
import RSocketWebSocketClient from 'rsocket-websocket-client';

const getDevToolsWsUrl = () => {
    // DevTools RSocket WebSocket endpoint - backend runs on port 8080
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.hostname;
    // In development, Vite runs on 3000 but backend is on 8080
    // In production, both would be on the same port
    const port = (window.location.port === '3000' || window.location.port === '5173') ? '8080' : window.location.port;
    return `${protocol}//${host}:${port}/devtools-ws`;
};

export class RSocketService {
    private client: any;

    connect(): Promise<void> {
        const wsUrl = getDevToolsWsUrl();
        return new Promise((resolve, reject) => {
            const client = new RSocketClient({
                serializers: {
                    data: IdentitySerializer,
                    metadata: IdentitySerializer,
                },
                setup: {
                    keepAlive: 60000,
                    lifetime: 180000,
                    dataMimeType: 'text/plain',
                    metadataMimeType: 'text/plain',
                },
                transport: new RSocketWebSocketClient({
                    url: wsUrl,
                    wsCreator: (url: string) => new WebSocket(url),
                }),
            });

            client.connect().subscribe({
                onComplete: (socket: any) => {
                    this.client = socket;
                    console.log('Connected to DevTools RSocket at', wsUrl);
                    resolve();
                },
                onError: (error: any) => {
                    console.error('DevTools RSocket connection failed', error);
                    reject(error);
                },
                onSubscribe: (_cancel: any) => { }
            });
        });
    }

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

    requestStream(
        message: string,
        onNext: (data: any) => void,
        onError?: (error: any) => void
    ): { unsubscribe: () => void } {
        if (!this.client) {
            console.error("Client not connected");
            return { unsubscribe: () => { } };
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
            }
        };
    }

    disconnect() {
        if (this.client) {
            this.client.close();
            this.client = null;
        }
    }
}

export const rsocketService = new RSocketService();
