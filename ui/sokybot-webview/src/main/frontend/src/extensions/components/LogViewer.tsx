import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useVirtualizer } from '@tanstack/react-virtual';
import { format, formatDistanceToNow } from 'date-fns';
import { Button, cn, Input } from '@sokybot/frontend-shared';

interface LogViewerProps {
    events: any[];
    className?: string;
    style?: React.CSSProperties;
}

const ALL_LEVELS = ['ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'] as const;
type LogLevel = typeof ALL_LEVELS[number];

function normalizeLevel(entry: any): string {
    const level = String(entry?.level || 'INFO').toUpperCase();
    return level === 'AUDIT' ? 'INFO' : level;
}

function levelClasses(level: string): string {
    switch (level) {
        case 'ERROR':
            return 'bg-red-500/25 text-red-400 border border-red-500/40';
        case 'WARN':
            return 'bg-amber-500/25 text-amber-400 border border-amber-500/40';
        case 'DEBUG':
            return 'bg-sky-500/25 text-sky-400 border border-sky-500/40';
        case 'TRACE':
            return 'bg-zinc-500/25 text-zinc-400 border border-zinc-500/40';
        default:
            return 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40';
    }
}

function messageClasses(level: string): string {
    switch (level) {
        case 'ERROR':
            return 'border-red-500/50 bg-red-500/5';
        case 'WARN':
            return 'border-amber-500/50 bg-amber-500/5';
        case 'DEBUG':
            return 'border-sky-500/50 bg-sky-500/5';
        case 'TRACE':
            return 'border-zinc-500/50 bg-zinc-500/5';
        default:
            return 'border-emerald-500/30 bg-emerald-500/5';
    }
}

function toDate(timestamp: any): Date | null {
    if (!timestamp) return null;
    const date = new Date(timestamp);
    return Number.isNaN(date.getTime()) ? null : date;
}

function escapeRegex(value: string): string {
    return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function formatLogTime(date: Date | null): string {
    if (!date) return '—';
    return format(date, 'HH:mm:ss.SSS');
}

function formatLogRelative(date: Date | null): string {
    if (!date) return '';
    return formatDistanceToNow(date, { addSuffix: true });
}

function formatEventLine(entry: any): string {
    const level = normalizeLevel(entry);
    const time = formatLogTime(toDate(entry?.timestamp));
    const category = String(entry?.category || 'SYSTEM').toUpperCase();
    const source = String(entry?.source || 'unknown');
    const shortSource = source.split('.').pop() || source;
    const message = String(entry?.message || '');
    return `[${time}] ${level} [${category}] ${shortSource} - ${message}`;
}

async function copyText(payload: string): Promise<void> {
    if (navigator.clipboard && typeof navigator.clipboard.writeText === 'function') {
        await navigator.clipboard.writeText(payload);
        return;
    }
    const area = document.createElement('textarea');
    area.value = payload;
    area.setAttribute('readonly', 'true');
    area.style.position = 'absolute';
    area.style.left = '-9999px';
    document.body.appendChild(area);
    area.select();
    document.execCommand('copy');
    document.body.removeChild(area);
}

function highlightText(value: string, query: string): React.ReactNode {
    if (!query.trim()) return value;
    const normalizedQuery = query.trim();
    const safeQuery = escapeRegex(normalizedQuery);
    if (!safeQuery) return value;
    const matcher = new RegExp(`(${safeQuery})`, 'ig');
    const parts = value.split(matcher);
    return parts.map((part, idx) => (
        part.toLowerCase() === normalizedQuery.toLowerCase()
            ? <mark key={`${part}-${idx}`} className="bg-yellow-300/30 text-inherit px-0.5 rounded">{part}</mark>
            : <React.Fragment key={`${part}-${idx}`}>{part}</React.Fragment>
    ));
}

export const LogViewer: React.FC<LogViewerProps> = ({ events, className, style }) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const [paused, setPaused] = useState(false);
    const [showJumpToTop, setShowJumpToTop] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [enabledLevels, setEnabledLevels] = useState<Set<LogLevel>>(new Set<LogLevel>(ALL_LEVELS));
    const [expanded, setExpanded] = useState<Record<string, boolean>>({});
    const [frozenEvents, setFrozenEvents] = useState<any[]>(events);
    const [missedCount, setMissedCount] = useState(0);
    const [copyStatus, setCopyStatus] = useState<'idle' | 'copied' | 'error'>('idle');

    useEffect(() => {
        if (paused) {
            setMissedCount(prev => prev + 1);
        } else {
            setFrozenEvents(events);
        }
    }, [events, paused]);

    useEffect(() => {
        const node = containerRef.current;
        if (!node) return;

        const onScroll = () => setShowJumpToTop(node.scrollTop > 200);
        node.addEventListener('scroll', onScroll);
        return () => {
            node.removeEventListener('scroll', onScroll);
        };
    }, []);

    const baseEvents = paused ? frozenEvents : events;
    const searchedEvents = useMemo(() => {
        const query = searchQuery.trim().toLowerCase();
        if (!query) return baseEvents;
        return baseEvents.filter(entry => {
            const fields = [
                String(entry?.message || ''),
                String(entry?.source || ''),
                String(entry?.category || ''),
                String(entry?.stackTrace || '')
            ];
            return fields.some(field => field.toLowerCase().includes(query));
        });
    }, [baseEvents, searchQuery]);

    const displayedEvents = useMemo(() => {
        return searchedEvents.filter(entry => enabledLevels.has(normalizeLevel(entry) as LogLevel));
    }, [searchedEvents, enabledLevels]);

    const rowVirtualizer = useVirtualizer({
        count: displayedEvents.length,
        getScrollElement: () => containerRef.current,
        estimateSize: () => 88,
        overscan: 12,
        getItemKey: (index) => {
            const item = displayedEvents[index];
            return String(item?.id ?? `${index}-${item?.timestamp ?? ''}`);
        },
    });

    useEffect(() => {
        if (!paused && displayedEvents.length > 0) {
            rowVirtualizer.scrollToOffset(0, { align: 'start' });
        }
    }, [frozenEvents, paused, displayedEvents.length, rowVirtualizer]);

    const counts = useMemo(() => {
        const byLevel: Record<string, number> = {
            ERROR: 0,
            WARN: 0,
            INFO: 0,
            DEBUG: 0,
            TRACE: 0
        };
        for (const event of baseEvents) {
            const level = normalizeLevel(event);
            if (byLevel[level] == null) byLevel[level] = 0;
            byLevel[level] += 1;
        }
        return byLevel;
    }, [baseEvents]);

    const applyCopyStatus = (status: 'copied' | 'error') => {
        setCopyStatus(status);
        window.setTimeout(() => setCopyStatus('idle'), status === 'copied' ? 1400 : 1800);
    };

    const copyFilteredEvents = async () => {
        const payload = displayedEvents.map(formatEventLine).join('\n');
        try {
            await copyText(payload);
            applyCopyStatus('copied');
        } catch {
            applyCopyStatus('error');
        }
    };

    const copyFilteredAsJson = async () => {
        try {
            await copyText(JSON.stringify(displayedEvents, null, 2));
            applyCopyStatus('copied');
        } catch {
            applyCopyStatus('error');
        }
    };

    const copySingleEntry = async (entry: any) => {
        try {
            await copyText(formatEventLine(entry));
            applyCopyStatus('copied');
        } catch {
            applyCopyStatus('error');
        }
    };

    const toggleLevel = (level: LogLevel) => {
        setEnabledLevels(prev => {
            const next = new Set(prev);
            if (next.has(level)) {
                next.delete(level);
            } else {
                next.add(level);
            }
            return next;
        });
    };

    const togglePause = () => {
        setPaused(prev => {
            const next = !prev;
            if (!next) {
                setMissedCount(0);
            }
            return next;
        });
    };

    const jumpToLatest = () => {
        rowVirtualizer.scrollToOffset(0, { align: 'start' });
        setShowJumpToTop(false);
    };

    return (
        <div className={cn('h-full min-h-0 flex flex-col gap-2', className)} style={style}>
            <div className="flex items-center gap-2 text-[11px] text-muted-foreground flex-wrap">
                <span className="font-medium">Events: {displayedEvents.length}</span>
                <Button type="button" variant="outline" size="sm" onClick={() => toggleLevel('ERROR')} className={cn('h-7 px-1.5 text-[11px] border-red-500/40 text-red-400', !enabledLevels.has('ERROR') && 'opacity-55')}>
                    E {counts.ERROR || 0}
                </Button>
                <Button type="button" variant="outline" size="sm" onClick={() => toggleLevel('WARN')} className={cn('h-7 px-1.5 text-[11px] border-amber-500/40 text-amber-400', !enabledLevels.has('WARN') && 'opacity-55')}>
                    W {counts.WARN || 0}
                </Button>
                <Button type="button" variant="outline" size="sm" onClick={() => toggleLevel('INFO')} className={cn('h-7 px-1.5 text-[11px] border-emerald-500/40 text-emerald-400', !enabledLevels.has('INFO') && 'opacity-55')}>
                    I {counts.INFO || 0}
                </Button>
                <Button type="button" variant="outline" size="sm" onClick={() => toggleLevel('DEBUG')} className={cn('h-7 px-1.5 text-[11px] border-sky-500/40 text-sky-400', !enabledLevels.has('DEBUG') && 'opacity-55')}>
                    D {counts.DEBUG || 0}
                </Button>
                <Button type="button" variant="outline" size="sm" onClick={() => toggleLevel('TRACE')} className={cn('h-7 px-1.5 text-[11px] border-zinc-500/40 text-zinc-400', !enabledLevels.has('TRACE') && 'opacity-55')}>
                    T {counts.TRACE || 0}
                </Button>
                <Input
                    type="text"
                    className="ml-1 h-7 min-h-7 py-1 text-[11px] min-w-52"
                    placeholder="Search logs..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                />
                <Button type="button" variant="outline" size="sm" onClick={copyFilteredEvents} className="ml-auto h-7 text-[11px]">
                    Copy Filtered
                </Button>
                <Button type="button" variant="outline" size="sm" onClick={copyFilteredAsJson} className="h-7 text-[11px]">
                    Copy JSON
                </Button>
                <Button type="button" variant="outline" size="sm" onClick={togglePause} className="h-7 text-[11px]">
                    {paused ? 'Resume' : 'Pause'}
                </Button>
                {paused && missedCount > 0 && <span className="text-amber-300">+{missedCount} new</span>}
                {copyStatus === 'copied' && <span className="text-emerald-400">Copied</span>}
                {copyStatus === 'error' && <span className="text-red-400">Copy failed</span>}
            </div>

            <div
                ref={containerRef}
                className="relative bg-muted/30 border border-border text-foreground p-2 rounded overflow-y-auto font-mono text-xs shadow-inner flex-1 min-h-0"
            >
                {displayedEvents.length === 0 && (
                    <div className="mt-10 text-center text-muted-foreground">Waiting for events...</div>
                )}

                {displayedEvents.length > 0 && (
                    <div
                        style={{
                            height: `${rowVirtualizer.getTotalSize()}px`,
                            width: '100%',
                            position: 'relative',
                        }}
                    >
                        {rowVirtualizer.getVirtualItems().map((virtualRow) => {
                            const item = displayedEvents[virtualRow.index];
                            const idx = virtualRow.index;
                            const level = normalizeLevel(item);
                            const time = toDate(item?.timestamp);
                            const source = String(item?.source || '');
                            const shortSource = source.split('.').pop() || source;
                            const message = String(item?.message || '');
                            const category = String(item?.category || '');
                            const stackTrace = String(item?.stackTrace || '');
                            const thread = String(item?.thread || '');
                            const metadata = item?.metadata && typeof item.metadata === 'object' ? item.metadata : null;
                            const entryId = String(item?.id || `${idx}-${item?.timestamp || ''}`);
                            const isExpanded = Boolean(expanded[entryId]);

                            return (
                                <div
                                    key={virtualRow.key}
                                    data-index={virtualRow.index}
                                    ref={rowVirtualizer.measureElement}
                                    style={{
                                        position: 'absolute',
                                        top: 0,
                                        left: 0,
                                        width: '100%',
                                        transform: `translateY(${virtualRow.start}px)`,
                                    }}
                                    className="border-b border-border/80 py-2.5 px-2 hover:bg-muted/40 transition-colors flex flex-col gap-1.5 rounded-sm"
                                >
                                    <div className="items-center gap-2 flex flex-wrap">
                                        <span className="text-muted-foreground text-[11px] whitespace-nowrap tabular-nums">
                                            {formatLogTime(time)}
                                        </span>
                                        <span className="text-[10px] text-muted-foreground whitespace-nowrap">
                                            {formatLogRelative(time)}
                                        </span>
                                        <span className={cn('px-2 py-0.5 rounded text-[10px] font-semibold tracking-wide uppercase', levelClasses(level))}>
                                            {level}
                                        </span>
                                        {category && (
                                            <span className="text-[10px] text-muted-foreground uppercase font-medium">
                                                {category}
                                            </span>
                                        )}
                                        {source && (
                                            <span className="text-[10px] text-muted-foreground ml-auto truncate max-w-[280px] text-right" title={source}>
                                                {shortSource}
                                            </span>
                                        )}
                                    </div>

                                    <div className={cn('pl-2.5 text-foreground text-[12px] break-words leading-snug border-l-2 rounded-sm', messageClasses(level))}>
                                        {highlightText(message, searchQuery)}
                                    </div>

                                    <div className="flex items-center gap-2">
                                        {(stackTrace || source) && (
                                            <Button
                                                type="button"
                                                variant="link"
                                                className="text-[10px] h-auto p-0 text-muted-foreground"
                                                onClick={() => setExpanded(prev => ({ ...prev, [entryId]: !isExpanded }))}
                                            >
                                                {isExpanded ? 'Hide details' : 'Show details'}
                                            </Button>
                                        )}
                                        <Button
                                            type="button"
                                            variant="link"
                                            className="text-[10px] h-auto p-0 text-muted-foreground"
                                            onClick={() => copySingleEntry(item)}
                                        >
                                            Copy entry
                                        </Button>
                                    </div>

                                    {isExpanded && (
                                        <div className="text-[11px] border border-border/70 rounded p-2 bg-background/40 space-y-1">
                                            <div>
                                                <span className="text-muted-foreground">Id: </span>
                                                <span className="break-all">{entryId}</span>
                                            </div>
                                            {source && (
                                                <div>
                                                    <span className="text-muted-foreground">Source: </span>
                                                    <span className="break-all">{source}</span>
                                                </div>
                                            )}
                                            {thread && (
                                                <div>
                                                    <span className="text-muted-foreground">Thread: </span>
                                                    <span className="break-all">{thread}</span>
                                                </div>
                                            )}
                                            {metadata && Object.keys(metadata).length > 0 && (
                                                <div>
                                                    <span className="text-muted-foreground">Metadata:</span>
                                                    <div className="pl-3 space-y-0.5">
                                                        {Object.entries(metadata).map(([key, value]) => (
                                                            <div key={key}>
                                                                <span className="text-muted-foreground">{key}: </span>
                                                                <span className="break-all">{String(value)}</span>
                                                            </div>
                                                        ))}
                                                    </div>
                                                </div>
                                            )}
                                            {stackTrace && (
                                                <pre className="whitespace-pre-wrap break-words text-[10px] max-h-64 overflow-y-auto">
                                                    {stackTrace}
                                                </pre>
                                            )}
                                        </div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                )}
                {showJumpToTop && (
                    <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={jumpToLatest}
                        className="absolute right-3 bottom-3 h-8 text-[11px] bg-background/90"
                    >
                        Jump to latest
                    </Button>
                )}
            </div>
        </div>
    );
};

