declare module 'rsocket-core' {
  export const IdentitySerializer: any;

  export interface RSocketClientConfig {
    serializers?: {
      data: any;
      metadata: any;
    };
    setup: {
      keepAlive?: number;
      lifetime?: number;
      dataMimeType?: string;
      metadataMimeType?: string;
    };
    transport: any;
  }

  export class RSocketClient {
    constructor(config: RSocketClientConfig);
    connect(): {
      subscribe: (handlers: {
        onComplete?: (socket: any) => void;
        onError?: (error: any) => void;
        onSubscribe?: (cancel: () => void) => void;
      }) => void;
    };
  }

  export interface RSocket {
    requestResponse(payload: {
      data: string;
      metadata: string;
    }): {
      subscribe: (handlers: {
        onComplete?: (response: any) => void;
        onError?: (error: any) => void;
      }) => void;
    };
    requestStream(payload: {
      data: string;
      metadata: string;
    }): {
      subscribe: (handlers: {
        onNext?: (payload: any) => void;
        onError?: (error: any) => void;
        onSubscribe?: (subscription: any) => void;
      }) => void;
    };
    close?(): void;
  }
}
