
const { RSocketClient } = require('rsocket-core');
const RSocketWebSocketClient = require('rsocket-websocket-client').default;
const WebSocket = require('ws');

async function testConnection() {
    console.log("Attempting to connect to RSocket server...");

    const client = new RSocketClient({
        setup: {
            keepAlive: 60000,
            lifetime: 180000,
            dataMimeType: 'text/plain',
            metadataMimeType: 'text/plain',
        },
        transport: new RSocketWebSocketClient({
            // Connect to RSocket WebSocket endpoint on HTTP server
            url: 'ws://localhost:8080/rsocket',
            wsCreator: (url) => new WebSocket(url),
            debug: true
        }),
    });

    try {
        const rsocket = await new Promise((resolve, reject) => {
            client.connect().subscribe({
                onComplete: socket => resolve(socket),
                onError: error => reject(error),
            });
        });

        console.log("Connected to RSocket server!");

        const payload = {
            data: "Hello from Verification Script",
            metadata: ""
        };

        const response = await new Promise((resolve, reject) => {
            rsocket.requestResponse(payload).subscribe({
                onComplete: (payload) => resolve(payload),
                onError: error => reject(error)
            });
        });

        console.log("Received response:", response.data);

        process.exit(0);
    } catch (error) {
        console.error("Failed to connect:", error);
        process.exit(1);
    }
}

testConnection();
