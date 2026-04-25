import { format } from 'date-fns';
import { useCallback, useLayoutEffect, useRef, useState } from 'react';
import { Button, cn, Switch } from '@sokybot/frontend-shared';
import type { SocialChannel } from '../../RSocketClient';
import type { ChatLineDto } from '../../RSocketClient';
import { useSocialChatStream } from '../../hooks/useSocialChatStream';
import { useSocialStore } from '../../store/socialStore';

const FILTER_CHANNELS: SocialChannel[] = [
    'ALL',
    'PARTY',
    'GUILD',
    'UNION',
    'PRIVATE',
    'STALL',
    'GLOBAL',
    'ACADEMY',
    'NOTICE',
];

const RENDER_WINDOW = 200;

function channelTint(channel: string): string {
    switch (channel) {
        case 'PARTY':
            return 'text-sky-400';
        case 'GUILD':
            return 'text-emerald-400';
        case 'PRIVATE':
            return 'text-violet-400';
        case 'GLOBAL':
            return 'text-amber-400';
        case 'NOTICE':
            return 'text-orange-400';
        default:
            return 'text-muted-foreground';
    }
}

function ChatLineRow({ line }: { line: ChatLineDto }) {
    const ts = format(new Date(line.timestampEpochMs), 'HH:mm:ss');
    const senderClass = line.fromGameMaster
        ? 'text-red-400 font-semibold'
        : line.fromSelf
          ? 'text-emerald-400 font-medium'
          : channelTint(line.channel);

    return (
        <div className="flex gap-2 border-b border-border/40 py-1 text-xs leading-snug">
            <span className="shrink-0 font-mono text-[10px] text-muted-foreground" title={line.timestampEpochMs.toString()}>
                {ts}
            </span>
            <span className={cn('shrink-0 font-mono text-[10px]', channelTint(line.channel))}>{line.channel}</span>
            <span className={cn('shrink-0 max-w-[7rem] truncate', senderClass)} title={line.senderName}>
                {line.senderName}
            </span>
            <span className="min-w-0 flex-1 whitespace-pre-wrap break-words font-mono text-[11px] text-foreground/90">
                {line.message}
            </span>
        </div>
    );
}

function ChannelFilterBar() {
    const channels = useSocialStore((s) => s.filters.channels);
    const toggle = useSocialStore((s) => s.toggleFilterChannel);

    return (
        <div className="flex flex-wrap gap-1.5 border-b border-border/60 pb-3">
            <span className="w-full text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">Channels</span>
            {channels.size === 0 ? (
                <span className="text-[10px] italic text-muted-foreground">All channels (server)</span>
            ) : null}
            {FILTER_CHANNELS.map((ch) => {
                const on = channels.has(ch);
                return (
                    <Button
                        key={ch}
                        type="button"
                        size="sm"
                        variant={on ? 'secondary' : 'outline'}
                        className="h-7 px-2 text-[10px] font-mono"
                        onClick={() => toggle(ch)}
                    >
                        {ch}
                    </Button>
                );
            })}
        </div>
    );
}

export interface LiveChatPanelProps {
    machineId: string;
}

export function LiveChatPanel({ machineId }: LiveChatPanelProps) {
    useSocialChatStream(machineId);
    const lines = useSocialStore((s) => s.chatByMachine[machineId] ?? []);
    const includeSelf = useSocialStore((s) => s.filters.includeSelf);
    const includeGm = useSocialStore((s) => s.filters.includeGm);
    const setFilters = useSocialStore((s) => s.setFilters);

    const scrollRef = useRef<HTMLDivElement>(null);
    const [tailLocked, setTailLocked] = useState(true);

    const displayed = lines.slice(-RENDER_WINDOW);

    const onScroll = useCallback(() => {
        const el = scrollRef.current;
        if (!el) return;
        const dist = el.scrollHeight - el.scrollTop - el.clientHeight;
        setTailLocked(dist <= 40);
    }, []);

    useLayoutEffect(() => {
        const el = scrollRef.current;
        if (!el || !tailLocked) return;
        el.scrollTop = el.scrollHeight;
    }, [displayed.length, tailLocked, machineId]);

    useLayoutEffect(() => {
        setTailLocked(true);
    }, [machineId]);

    return (
        <div className="flex min-h-0 min-w-0 flex-1 flex-col gap-2">
            <div className="flex flex-wrap items-center gap-4 border-b border-border/40 pb-2">
                <label className="flex items-center gap-2 text-[11px] text-muted-foreground">
                    <Switch
                        checked={includeGm}
                        onCheckedChange={(v: boolean) => setFilters({ includeGm: Boolean(v) })}
                    />
                    Show GM lines
                </label>
                <label className="flex items-center gap-2 text-[11px] text-muted-foreground">
                    <Switch
                        checked={includeSelf}
                        onCheckedChange={(v: boolean) => setFilters({ includeSelf: Boolean(v) })}
                    />
                    Show self
                </label>
            </div>
            <ChannelFilterBar />
            <div className="relative min-h-0 flex-1">
                {!tailLocked ? (
                    <div className="absolute right-2 top-2 z-10 rounded-full bg-secondary px-2 py-0.5 text-[10px] text-secondary-foreground shadow">
                        Live tail paused
                    </div>
                ) : null}
                <div
                    ref={scrollRef}
                    onScroll={onScroll}
                    className="h-full max-h-[calc(100vh-14rem)] overflow-y-auto rounded-md border border-border/60 bg-background/40 p-2"
                >
                    {displayed.length === 0 ? (
                        <div className="py-8 text-center text-xs italic text-muted-foreground">Waiting for chat…</div>
                    ) : (
                        displayed.map((line, i) => (
                            <ChatLineRow
                                key={`${line.timestampEpochMs}-${line.senderName}-${i}`}
                                line={line}
                            />
                        ))
                    )}
                </div>
            </div>
            {lines.length > RENDER_WINDOW ? (
                <div className="text-[10px] text-muted-foreground">
                    Showing last {RENDER_WINDOW} of {lines.length} buffered lines
                </div>
            ) : null}
        </div>
    );
}
