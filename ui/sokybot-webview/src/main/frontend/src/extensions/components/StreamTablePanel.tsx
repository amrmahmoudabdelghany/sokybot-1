import React, { useMemo } from 'react';
import { cn, Card, CardContent, CardHeader, CardTitle, Input, Button, Badge, Separator } from '@sokybot/frontend-shared';
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
    const trafficToolbar =
        binding.streamId === 'packets' || stateKey === 'trafficPackets';
    const trafficAutoScroll = context.trafficAutoScroll !== false;

    const selectedPacketIndices = useMemo(() => {
        const raw = context.selectedPacketIndices;
        if (!Array.isArray(raw)) return [];
        return raw.map((x) => Number(x)).filter((n) => Number.isFinite(n) && n >= 0);
    }, [context.selectedPacketIndices]);
    const canDiffPackets = selectedPacketIndices.length >= 2;

    const subtitle = useMemo(() => {
        const capacity = binding.maxCapacity ?? binding.rollingWindow ?? 1000;
        return `${rows.length} rows · cap ${capacity}`;
    }, [rows.length, binding.maxCapacity, binding.rollingWindow]);

    const statusBadgeVariant =
        status === 'running' ? 'default' : status === 'paused' ? 'secondary' : 'outline';

    return (
        <Card className={cn('w-full shrink-0 flex flex-col border-border bg-card shadow-sm', className)} style={style}>
            <CardHeader className="space-y-0 pb-3">
                <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                    <div className="min-w-0 space-y-1">
                        <CardTitle className="text-base tracking-tight">{title}</CardTitle>
                        <p className="text-xs text-muted-foreground font-mono">{subtitle}</p>
                        {trafficToolbar && (
                            <p className="text-[11px] text-muted-foreground leading-snug max-w-xl">
                                To diff: Ctrl/Cmd-click two packets in the table (blue highlight), then{' '}
                                <span className="font-medium text-foreground/90">Compare packets</span>. If you pick more
                                than two, the two largest indices are compared.
                            </p>
                        )}
                    </div>
                    <div className="flex flex-wrap items-center gap-2 lg:justify-end">
                        <Badge
                            variant={isSubscribed ? 'default' : 'outline'}
                            className={cn(
                                'font-normal text-[10px] uppercase tracking-wide',
                                !isSubscribed && 'border-amber-500/50 text-amber-600 dark:text-amber-400'
                            )}
                        >
                            {isSubscribed ? 'Subscribed' : 'Not subscribed'}
                        </Badge>
                        <Badge variant={statusBadgeVariant} className="font-mono text-[10px] font-normal capitalize">
                            {transportState === 'disconnected' ? 'transport off' : status}
                        </Badge>
                        <Separator orientation="vertical" className="hidden h-6 sm:block" />
                        <div className="flex flex-wrap items-center gap-2">
                            <Button
                                variant="outline"
                                size="sm"
                                className="h-8"
                                onClick={() => onAction?.('togglePause', {})}
                            >
                                {context.monitorEnabled === false ? 'Resume' : 'Pause'}
                            </Button>
                            {trafficToolbar && (
                                <>
                                    <Button
                                        variant="default"
                                        size="sm"
                                        className="h-8"
                                        onClick={() => onAction?.('openAnalyzer', {})}
                                    >
                                        Packet analyzer
                                    </Button>
                                    <Button
                                        variant="secondary"
                                        size="sm"
                                        className="h-8"
                                        disabled={!canDiffPackets}
                                        title={
                                            canDiffPackets
                                                ? 'Open side-by-side hex diff for two selected packets'
                                                : 'Select at least two packets (Ctrl/Cmd-click rows)'
                                        }
                                        onClick={() =>
                                            onAction?.('diffPackets', { indices: selectedPacketIndices })
                                        }
                                    >
                                        Compare packets
                                    </Button>
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        className="h-8"
                                        onClick={() => onAction?.('toggleTrafficAutoScroll', {})}
                                    >
                                        {trafficAutoScroll ? 'Auto-scroll on' : 'Auto-scroll off'}
                                    </Button>
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        className="h-8"
                                        disabled={!hasRows}
                                        onClick={() => onAction?.('exportMonitorCsv', {})}
                                    >
                                        Export CSV
                                    </Button>
                                    <Button
                                        variant="destructive"
                                        size="sm"
                                        className="h-8"
                                        onClick={() => onAction?.('clearMonitor', {})}
                                    >
                                        Clear log
                                    </Button>
                                </>
                            )}
                            {reconnectVisible && (
                                <Button
                                    variant="default"
                                    size="sm"
                                    className="h-8"
                                    disabled={reconnectDisabled}
                                    onClick={() => onAction?.('ensurePacketSubscription', {})}
                                >
                                    Reconnect
                                </Button>
                            )}
                        </div>
                    </div>
                </div>
            </CardHeader>
            <CardContent className="p-0 flex flex-col border-t border-border">
                {!isProxyBound && (
                    <div className="px-4 pt-3 text-xs text-amber-600 dark:text-amber-400">
                        Proxy not bound. Click Reconnect after proxy becomes available.
                    </div>
                )}
                {context.lastBindError && (
                    <div className="px-4 pt-3 text-xs text-destructive">{String(context.lastBindError)}</div>
                )}
                {showHiddenWarning && (
                    <div className="mx-4 mt-3 rounded-md border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-xs text-amber-950 dark:text-amber-100 space-y-2">
                        <div>
                            {hasIgnoredDrops && hasFilterDrops && 'Packets are hidden by ignored tracers and the active monitor filter.'}
                            {hasIgnoredDrops && !hasFilterDrops && 'Packets are hidden because one or more tracers are ignored.'}
                            {!hasIgnoredDrops && hasFilterDrops && 'Packets are hidden by the active monitor filter.'}
                        </div>
                        <div className="text-amber-800/90 dark:text-amber-200/90">
                            Previously hidden packets are not replayed; new packets will appear after recovery.
                        </div>
                        <div className="flex flex-wrap items-center gap-2">
                            {hasFilterDrops && (
                                <Button
                                    variant="outline"
                                    size="sm"
                                    className="h-8"
                                    onClick={() => onAction?.('filterMonitor', { value: '' })}
                                >
                                    Clear filter
                                </Button>
                            )}
                            {hasIgnoredDrops && (
                                <Button
                                    variant="outline"
                                    size="sm"
                                    className="h-8"
                                    onClick={() => onAction?.('unignoreAllTracers', {})}
                                >
                                    Unignore all
                                </Button>
                            )}
                        </div>
                    </div>
                )}
                {(filterKey && filterAction) && (
                    <div className="flex flex-col gap-2 border-b border-border bg-muted/25 px-4 py-3 sm:flex-row sm:items-center">
                        <Input
                            className="h-9 flex-1"
                            value={filterValue}
                            onChange={(e) => onAction?.(filterAction, { value: e.target.value })}
                            placeholder="Filter stream…"
                        />
                        <Button
                            variant="secondary"
                            size="sm"
                            className="h-9 shrink-0 sm:w-auto"
                            onClick={() => onAction?.(filterAction, { value: '' })}
                        >
                            Clear
                        </Button>
                    </div>
                )}
                {!hasRows && (
                    <div className="px-4 py-6 text-sm text-muted-foreground">Waiting for stream data…</div>
                )}
            </CardContent>
        </Card>
    );
};
