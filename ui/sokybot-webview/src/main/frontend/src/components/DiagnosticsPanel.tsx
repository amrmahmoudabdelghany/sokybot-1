import React, { useCallback, useEffect, useRef, useState } from 'react';
import type { RSocketService } from '../RSocketClient';
import { useRSocketService } from '../RSocketProvider';
import { Button } from '@sokybot/frontend-shared';
import { Activity, ChevronDown, ChevronRight } from 'lucide-react';
import { cn } from '@sokybot/frontend-shared';

const MAX_LINES = 400;

/**
 * Collapsible RSocket diagnostics: live event-bridge stream via request-channel.
 */
export const DiagnosticsPanel: React.FC<{ isBackendConnected: boolean }> = ({ isBackendConnected }) => {
    const rsocket = useRSocketService();
    const [open, setOpen] = useState(false);
    const [running, setRunning] = useState(false);
    const [lines, setLines] = useState<string[]>([]);
    const [pattern, setPattern] = useState('sokybot/**');
    const [machineId, setMachineId] = useState('');
    const channelRef = useRef<ReturnType<RSocketService['openDiagnosticsChannel']> | null>(
        null,
    );

    const appendLine = useCallback((text: string) => {
        setLines((prev) => {
            const next = [...prev, text];
            if (next.length > MAX_LINES) {
                return next.slice(-MAX_LINES);
            }
            return next;
        });
    }, []);

    const stopChannel = useCallback(() => {
        channelRef.current?.close();
        channelRef.current = null;
        setRunning(false);
    }, []);

    const startChannel = useCallback(() => {
        stopChannel();
        setLines([]);
        const ch = rsocket.openDiagnosticsChannel({
            pattern: pattern.trim() || '**',
            machineId: machineId.trim() || undefined,
            onMessage: (data) => {
                try {
                    appendLine(JSON.stringify(data));
                } catch {
                    appendLine(String(data));
                }
            },
            onError: (err) => {
                appendLine(`[error] ${err instanceof Error ? err.message : String(err)}`);
            },
        });
        channelRef.current = ch;
        setRunning(true);
    }, [appendLine, machineId, pattern, rsocket, stopChannel]);

    useEffect(() => {
        return () => stopChannel();
    }, [stopChannel]);

    const sendCommand = (params: Record<string, unknown>) => {
        channelRef.current?.send(params);
    };

    if (!isBackendConnected) {
        return null;
    }

    return (
        <div className="border-t border-border bg-muted/30">
            <button
                type="button"
                className="w-full px-3 py-2 flex items-center gap-2 text-[10px] font-semibold text-muted-foreground hover:text-foreground hover:bg-muted/50"
                onClick={() => setOpen((o) => !o)}
            >
                {open ? <ChevronDown className="h-3 w-3" /> : <ChevronRight className="h-3 w-3" />}
                <Activity className="h-3 w-3" />
                RSocket diagnostics
            </button>
            {open && (
                <div className="px-3 pb-3 space-y-2">
                    <div className="flex flex-col gap-1">
                        <label className="text-[9px] uppercase text-muted-foreground">Topic pattern</label>
                        <input
                            className="text-[10px] font-mono bg-background border border-border rounded px-1.5 py-1"
                            value={pattern}
                            onChange={(e) => setPattern(e.target.value)}
                            disabled={running}
                        />
                    </div>
                    <div className="flex flex-col gap-1">
                        <label className="text-[9px] uppercase text-muted-foreground">Machine id (optional)</label>
                        <input
                            className="text-[10px] font-mono bg-background border border-border rounded px-1.5 py-1"
                            value={machineId}
                            onChange={(e) => setMachineId(e.target.value)}
                            disabled={running}
                            placeholder="Group.Machine"
                        />
                    </div>
                    <div className="flex flex-wrap gap-1">
                        {!running ? (
                            <Button size="sm" variant="secondary" className="h-7 text-[10px]" onClick={startChannel}>
                                Start stream
                            </Button>
                        ) : (
                            <Button size="sm" variant="outline" className="h-7 text-[10px]" onClick={stopChannel}>
                                Stop
                            </Button>
                        )}
                        <Button
                            size="sm"
                            variant="ghost"
                            className="h-7 text-[10px]"
                            disabled={!running}
                            onClick={() => sendCommand({ action: 'pause' })}
                        >
                            Pause
                        </Button>
                        <Button
                            size="sm"
                            variant="ghost"
                            className="h-7 text-[10px]"
                            disabled={!running}
                            onClick={() => sendCommand({ action: 'resume' })}
                        >
                            Resume
                        </Button>
                        <Button
                            size="sm"
                            variant="ghost"
                            className="h-7 text-[10px]"
                            disabled={!running}
                            onClick={() => sendCommand({ action: 'ping' })}
                        >
                            Ping
                        </Button>
                    </div>
                    <pre
                        className={cn(
                            'text-[9px] font-mono leading-tight max-h-40 overflow-auto rounded border border-border/60',
                            'bg-background/80 p-2 whitespace-pre-wrap break-all',
                        )}
                    >
                        {lines.length === 0 ? 'No events yet.' : lines.join('\n')}
                    </pre>
                </div>
            )}
        </div>
    );
};
