declare module 'rsocket-websocket-client' {
  export interface RSocketWebSocketClientConfig {
    url: string;
    wsCreator: (url: string) => WebSocket;
  }

  export default class RSocketWebSocketClient {
    constructor(config: RSocketWebSocketClientConfig);
  }
}
