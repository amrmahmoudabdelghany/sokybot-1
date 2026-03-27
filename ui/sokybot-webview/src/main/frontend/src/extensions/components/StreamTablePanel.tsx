import React, { useMemo } from 'react';
import { cn, Card, CardContent, CardHeader, CardTitle, Input, Button } from '@sokybot/frontend-shared';
import type { StreamBindingConfig } from '../ui-types';

interface StreamTablePanelProps {
    className?: string;
    style?: React.CSSProperties;
    context?: Record<string, any>;
    onAction?: (action: string, data: any) => Promise<any>;
    binding: StreamBindingConfig;
    title?: string;
    filterKey?: string;
    filterAction?: string;
}

export const StreamTablePanel: React.FC<StreamTablePanelProps> = ({
    className,
    style,
    context = {},
    onAction,
    binding,
    title = 'Live Stream',
    filterKey,
    filterAction,
}) => {
    const stateKey = binding.stateKey || binding.streamId;
    const rows = Array.isArray(context[stateKey]) ? context[stateKey] : [];
    const filterValue = filterKey ? String(context[filterKey] ?? '') : '';
    const hasRows = rows.length > 0;
    const health = (context.streamHealth && typeof context.streamHealth === 'object')
        ? context.streamHealth[binding.streamId]
        : null;
    const status = String(health?.status || context.bindingState || (context.monitorEnabled === false ? 'paused' : 'running'));
    const transportState = String(context.transportState || 'connected');
    const isProxyBound = context.isProxyBound !== false;
    const isSubscribed = context.isSubscribed === true;
    const totalPacketsSeen = Number(context.totalPacketsSeen ?? 0);
    const tracerCount = Number(context.tracerCount ?? 0);
    const droppedIgnoredCount = Number(context.droppedIgnoredCount ?? 0);
    const droppedFilterCount = Number(context.droppedFilterCount ?? 0);
    const hasIgnoredDrops = droppedIgnoredCount > 0;
    const hasFilterDrops = droppedFilterCount > 0;
    const showHiddenWarning = !hasRows
        && (totalPacketsSeen > 0 || tracerCount > 0)
        && (hasIgnoredDrops || hasFilterDrops);
    const reconnectVisible = !isSubscribed;
    const reconnectDisabled = status === 'binding' || transportState === 'disconnected';

    const subtitle = useMemo(() => {
        const capacity = binding.maxCapacity ?? binding.rollingWindow ?? 1000;
        return `${rows.length} rows (cap ${capacity})`;
    }, [rows.length, binding.maxCapacity, binding.rollingWindow]);

    return (
        <Card className={cn('w-full flex-shrink-0 flex flex-col', className)} style={style}>
            <CardHeader>
                <div className="flex items-center justify-between gap-2">
                    <CardTitle>{title}</CardTitle>
                    <div className="flex items-center gap-3">
                        <div className="text-xs text-muted-foreground font-mono">{subtitle}</div>
                        <div className="text-xs px-2 py-0.5 rounded border border-border text-foreground/80">
                            {transportState === 'disconnected' ? 'transport_disconnected' : status}
                        </div>
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => onAction?.('togglePause', {})}
                        >
                            {context.monitorEnabled === false ? 'Resume' : 'Pause'}
                        </Button>
                        {reconnectVisible && (
                            <Button
                                variant="default"
                                size="sm"
                                disabled={reconnectDisabled}
                                onClick={() => onAction?.('ensurePacketSubscription', {})}
                            >
                                Reconnect
                            </Button>
                        )}
                    </div>
                </div>
            </CardHeader>
            <CardContent className="p-0 flex flex-col">
                {!isProxyBound && (
                    <div className="px-3 pt-2 text-xs text-amber-400">
                        Proxy not bound. Click Reconnect after proxy becomes available.
                    </div>
                )}
                {context.lastBindError && (
                    <div className="px-3 pt-2 text-xs text-red-400">{String(context.lastBindError)}</div>
                )}
                {showHiddenWarning && (
                    <div className="mx-3 mt-3 rounded border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-xs text-amber-200 space-y-2">
                        <div>
                            {hasIgnoredDrops && hasFilterDrops && 'Packets are hidden by ignored tracers and the active monitor filter.'}
                            {hasIgnoredDrops && !hasFilterDrops && 'Packets are hidden because one or more tracers are ignored.'}
                            {!hasIgnoredDrops && hasFilterDrops && 'Packets are hidden by the active monitor filter.'}
                        </div>
                        <div className="text-amber-300/80">
                            Previously hidden packets are not replayed; new packets will appear after recovery.
                        </div>
                        <div className="flex items-center gap-2">
                            {hasFilterDrops && (
                                <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => onAction?.('filterMonitor', { value: '' })}
                                >
                                    Clear Filter
                                </Button>
                            )}
                            {hasIgnoredDrops && (
                                <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => onAction?.('unignoreAllTracers', {})}
                                >
                                    Unignore All
                                </Button>
                            )}
                        </div>
                    </div>
                )}
                {(filterKey && filterAction) && (
                    <div className="px-3 pt-3 flex items-center gap-2">
                        <Input
                            value={filterValue}
                            onChange={(e) => onAction?.(filterAction, { value: e.target.value })}
                            placeholder="Filter stream..."
                        />
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => onAction?.(filterAction, { value: '' })}
                        >
                            Clear
                        </Button>
                    </div>
                )}
                {!hasRows && (
                    <div className="p-4 text-sm text-muted-foreground">Waiting for stream data...</div>
                )}
            </CardContent>
        </Card>
    );
};
