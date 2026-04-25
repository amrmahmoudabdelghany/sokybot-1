import { create } from 'zustand';

export type ToastSeverity = 'info' | 'warn' | 'error';

export interface Toast {
    id: string;
    kind: string;
    title: string;
    body: string;
    severity: ToastSeverity;
    timestamp: number;
}

const MAX_VISIBLE = 5;

function newToastId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    return `toast-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

interface ToastSlice {
    toasts: Toast[];
    enqueue: (t: Omit<Toast, 'id'>) => void;
    dismiss: (id: string) => void;
}

export const useToastStore = create<ToastSlice>((set) => ({
    toasts: [],
    enqueue: (t) =>
        set((state) => {
            const toast: Toast = {
                ...t,
                id: newToastId(),
                timestamp: t.timestamp ?? Date.now(),
            };
            const next = [...state.toasts, toast];
            while (next.length > MAX_VISIBLE) {
                next.shift();
            }
            return { toasts: next };
        }),
    dismiss: (id) =>
        set((state) => ({
            toasts: state.toasts.filter((x) => x.id !== id),
        })),
}));
