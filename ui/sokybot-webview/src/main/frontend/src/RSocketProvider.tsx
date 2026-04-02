import React, { createContext, useContext, useEffect, useMemo, useRef } from 'react';
import { createRSocketService, RSocketService } from './RSocketClient';

const RSocketContext = createContext<RSocketService | null>(null);

export function RSocketProvider({ children, service }: { children: React.ReactNode; service?: RSocketService }) {
    const serviceRef = useRef<RSocketService | null>(service ?? null);
    if (!serviceRef.current) {
        serviceRef.current = createRSocketService();
    }
    const value = useMemo(() => serviceRef.current!, []);

    useEffect(() => {
        return () => {
            value.close();
            serviceRef.current = null;
        };
    }, [value]);

    return <RSocketContext.Provider value={value}>{children}</RSocketContext.Provider>;
}

export function useRSocketService(): RSocketService {
    const ctx = useContext(RSocketContext);
    if (!ctx) {
        throw new Error('useRSocketService must be used within RSocketProvider');
    }
    return ctx;
}
