import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

/** Fresh client per test; avoids retries and shared cache bleed. */
export function createTestQueryClient(): QueryClient {
    return new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    });
}

export function TestQueryProvider({
    children,
    client,
}: {
    children: React.ReactNode;
    client?: QueryClient;
}): React.ReactElement {
    const qc = client ?? createTestQueryClient();
    return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}
