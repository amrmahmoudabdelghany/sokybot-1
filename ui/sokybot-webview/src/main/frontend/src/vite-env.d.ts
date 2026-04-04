/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_BACKEND_URL?: string;
    readonly VITE_UI_BUILD_ID?: string;
}

declare module 'rsocket-core';
declare module 'rsocket-websocket-client';
