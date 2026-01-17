import { RSocketClient } from 'rsocket-core';
import RSocketWebSocketClient from 'rsocket-websocket-client';


export class RSocketService {
    private client: any;

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
                    url: `ws://${window.location.hostname}:7000`,
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
                    // response.data could be string or object depending on encoder? 
                    // With text/plain it should be string.
                    resolve(response.data);
                },
                onError: (error: any) => reject(error)
            });
        });
    }

    streamEvents(onEvent: (event: any) => void, onError: (error: any) => void) {
        if (!this.client) {
            console.error("Client not connected");
            return;
        }

        const payload = {
            data: "stream.events",
            metadata: ""
        };

        let subscription: any;
        this.client.requestStream(payload).subscribe({
            onNext: (payload: any) => {
                try {
                    const data = payload.data;
                    // Try to parse if it's a string, otherwise assume object
                    const event = typeof data === 'string' ? JSON.parse(data) : data;
                    onEvent(event);
                } catch (e) {
                    console.error("Failed to parse event", e);
                }
            },
            onError: (error: any) => onError(error),
            onSubscribe: (sub: any) => {
                subscription = sub;
                subscription.request(2147483647); // Request MAX int
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
    
    /**
     * Request a stream from the server
     */
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
                subscription.request(2147483647); // Request unlimited
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
    
    /**
     * Request a stream with automatic state updates
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
                // Auto-merge into state
                onStateUpdate({ [stateKey]: data });
            },
            onError
        );
    }
}

export const rsocketService = new RSocketService();
