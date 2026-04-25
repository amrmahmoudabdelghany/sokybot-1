import { useEffect } from 'react';
import { cn } from '@sokybot/frontend-shared';
import type { Toast } from '../../store/toastStore';
import { useToastStore } from '../../store/toastStore';

function severityClasses(severity: Toast['severity']): string {
    switch (severity) {
        case 'error':
            return 'border-red-500/60 bg-red-950/90 text-red-50';
        case 'warn':
            return 'border-amber-500/60 bg-amber-950/90 text-amber-50';
        default:
            return 'border-border bg-card/95 text-foreground';
    }
}

function ToastItem({ toast }: { toast: Toast }) {
    const dismiss = useToastStore((s) => s.dismiss);

    useEffect(() => {
        const id = window.setTimeout(() => dismiss(toast.id), 5000);
        return () => window.clearTimeout(id);
    }, [toast.id, dismiss]);

    return (
        <div
            role="status"
            className={cn(
                'pointer-events-auto rounded-lg border px-3 py-2 shadow-lg backdrop-blur-sm',
                'animate-in slide-in-from-right-4 fade-in duration-200',
                severityClasses(toast.severity)
            )}
        >
            <div className="flex justify-between gap-2">
                <div className="min-w-0">
                    <div className="text-xs font-semibold leading-tight">{toast.title}</div>
                    {toast.body ? (
                        <div className="mt-0.5 text-[11px] opacity-90 line-clamp-3">{toast.body}</div>
                    ) : null}
                    <div className="mt-1 text-[10px] opacity-60 font-mono">{toast.kind}</div>
                </div>
                <button
                    type="button"
                    className="shrink-0 rounded px-1 text-[10px] uppercase tracking-wide opacity-70 hover:opacity-100"
                    onClick={() => dismiss(toast.id)}
                    aria-label="Dismiss notification"
                >
                    ✕
                </button>
            </div>
        </div>
    );
}

/**
 * Fixed top-right stack (max 5) driven by {@link useToastStore}.
 */
export function ToastContainer() {
    const toasts = useToastStore((s) => s.toasts);

    return (
        <div
            className="pointer-events-none fixed right-4 top-4 z-[100] flex w-[min(100vw-2rem,22rem)] flex-col gap-2"
            aria-live="polite"
            aria-relevant="additions"
        >
            {toasts.map((t) => (
                <ToastItem key={t.id} toast={t} />
            ))}
        </div>
    );
}
