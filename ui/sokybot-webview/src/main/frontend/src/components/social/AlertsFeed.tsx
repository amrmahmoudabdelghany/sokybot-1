import { format } from 'date-fns';
import { cn } from '@sokybot/frontend-shared';
import { useSocialStore } from '../../store/socialStore';

function kindClass(kind: string): string {
    if (kind.startsWith('GM_') || kind === 'NOTICE_GM_BROADCAST') {
        return 'text-red-400';
    }
    if (kind.startsWith('HIVE_')) {
        if (kind === 'HIVE_ABORTED') {
            return 'text-red-400';
        }
        if (kind === 'HIVE_COMPLETED') {
            return 'text-emerald-400';
        }
        return 'text-yellow-300';
    }
    if (kind.startsWith('UNIQUE_')) {
        return 'text-amber-400';
    }
    return 'text-muted-foreground';
}

export function AlertsFeed() {
    const alerts = useSocialStore((s) => s.alerts);
    const visible = alerts.slice(-80).reverse();

    return (
        <div className="flex min-h-0 min-w-0 flex-1 flex-col gap-2">
            <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Alerts</h3>
            <div className="min-h-0 flex-1 overflow-y-auto rounded-md border border-border/60 bg-background/40 p-2">
                {visible.length === 0 ? (
                    <div className="py-6 text-center text-xs italic text-muted-foreground">No alerts yet</div>
                ) : (
                    <ul className="space-y-2">
                        {visible.map((a, idx) => (
                            <li
                                key={`${a.machineId}-${a.timestampEpochMs}-${a.kind}-${idx}`}
                                className="rounded border border-border/50 bg-card/40 px-2 py-1.5 text-[11px]"
                            >
                                <div className="flex justify-between gap-2 font-mono text-[10px] text-muted-foreground">
                                    <span>{format(new Date(a.timestampEpochMs), 'HH:mm:ss')}</span>
                                    <span className="truncate">{a.machineId}</span>
                                </div>
                                <div className={cn('mt-0.5 font-semibold', kindClass(a.kind))}>{a.kind}</div>
                                <div className="mt-0.5 text-foreground/90">{a.subject}</div>
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </div>
    );
}
