import { RSocketClient } from 'rsocket-core';
import RSocketWebSocketClient from 'rsocket-websocket-client';

export class RSocketService {
    private client: any;
    private readonly port: number = 7002; // Dev tools RSocket server port

    connect(): Promise<void> {
        return new Promise((resolve, reject) => {
            const client = new RSocketClient({
                setup: {
                    keepAlive: 60000,
                    lifetime: 180000,
                    dataMimeType: 'text/plain',
                    metadataMimeType: 'text/plain',
                },
                transport: new RSocketWebSocketClient({
                    url: `ws://${window.location.hostname}:${this.port}`,
                    wsCreator: (url: string) => new WebSocket(url),
                }),
            });

            client.connect().subscribe({
                onComplete: (socket: any) => {
                    this.client = socket;
                    console.log('Connected to DevTools RSocket on port', this.port);
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
            return { unsubscribe: () => {} };
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
