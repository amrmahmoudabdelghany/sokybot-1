import { useCallback, useEffect, useState } from 'react';
import { Button, cn } from '@sokybot/frontend-shared';
import { useRSocketService } from '../../RSocketProvider';

const WEBHOOK_SCOPE = 'webhooks';

const DEFAULT_KINDS = ['GM_NEARBY', 'GM_WHISPER', 'UNIQUE_SPAWNED', 'NOTICE_GM_BROADCAST'];

type SinkDraft = {
    id: string;
    kind: string;
    url: string;
    authToken: string;
    kindFilterCsv: string;
};

function splitMachineId(full: string): { group: string; name: string } {
    const i = full.indexOf('.');
    if (i <= 0) {
        return { group: 'default', name: full };
    }
    return { group: full.slice(0, i), name: full.slice(i + 1) };
}

function newSinkDraft(): SinkDraft {
    return {
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        kind: 'DISCORD',
        url: '',
        authToken: '',
        kindFilterCsv: '',
    };
}

function parseSinks(raw: unknown): SinkDraft[] {
    if (!Array.isArray(raw)) {
        return [];
    }
    return raw.map((row, idx) => {
        const o = row as Record<string, unknown>;
        const kf = o.kindFilter;
        let kindFilterCsv = '';
        if (Array.isArray(kf)) {
            kindFilterCsv = kf.map(String).join(',');
        } else if (kf && typeof kf === 'object') {
            kindFilterCsv = Array.from(Object.values(kf as Record<string, string>)).join(',');
        }
        return {
            id: `loaded-${idx}`,
            kind: String(o.kind ?? 'DISCORD'),
            url: String(o.url ?? ''),
            authToken: String(o.authToken ?? ''),
            kindFilterCsv,
        };
    });
}

function parseKinds(raw: unknown): string[] {
    let out: string[] = [];
    if (Array.isArray(raw)) {
        out = raw.map(String).filter(Boolean);
    } else if (raw && typeof raw === 'object') {
        out = Array.from(Object.values(raw as Record<string, string>)).filter(Boolean);
    }
    return out.length > 0 ? out : [...DEFAULT_KINDS];
}

export interface WebhookSettingsPanelProps {
    machineId: string;
}

export function WebhookSettingsPanel({ machineId }: WebhookSettingsPanelProps) {
    const rsocket = useRSocketService();
    const [enabled, setEnabled] = useState(false);
    const [dedupeMs, setDedupeMs] = useState(60_000);
    const [timeoutSec, setTimeoutSec] = useState(8);
    const [enabledKindsCsv, setEnabledKindsCsv] = useState(DEFAULT_KINDS.join(','));
    const [sinks, setSinks] = useState<SinkDraft[]>([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [testing, setTesting] = useState(false);
    const [message, setMessage] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);

    const load = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const raw = await rsocket.readMachineSettings(machineId, WEBHOOK_SCOPE);
            setEnabled(Boolean(raw.enabled));
            setDedupeMs(typeof raw.dedupeCooldownMs === 'number' ? raw.dedupeCooldownMs : Number(raw.dedupeCooldownMs) || 60_000);
            setTimeoutSec(
                typeof raw.requestTimeoutSeconds === 'number'
                    ? raw.requestTimeoutSeconds
                    : Number(raw.requestTimeoutSeconds) || 8
            );
            setEnabledKindsCsv(parseKinds(raw.enabledAlertKinds).join(','));
            const loaded = parseSinks(raw.sinks);
            setSinks(loaded.length > 0 ? loaded : []);
        } catch (e: unknown) {
            setError(e instanceof Error ? e.message : 'Failed to load webhook settings');
        } finally {
            setLoading(false);
        }
    }, [machineId, rsocket]);

    useEffect(() => {
        void load();
    }, [load]);

    const onSave = async () => {
        setSaving(true);
        setMessage(null);
        setError(null);
        try {
            const { group, name } = splitMachineId(machineId);
            const kinds = enabledKindsCsv
                .split(',')
                .map((s) => s.trim())
                .filter(Boolean);
            const payload: Record<string, unknown> = {
                enabled,
                dedupeCooldownMs: dedupeMs,
                requestTimeoutSeconds: timeoutSec,
                enabledAlertKinds: kinds,
                sinks: sinks.map((s) => ({
                    kind: s.kind,
                    url: s.url,
                    authToken: s.authToken,
                    kindFilter: s.kindFilterCsv
                        .split(',')
                        .map((x) => x.trim())
                        .filter(Boolean),
                })),
            };
            await rsocket.initializeMachine(group, name, WEBHOOK_SCOPE, payload);
            setMessage('Saved webhook settings.');
        } catch (e: unknown) {
            setError(e instanceof Error ? e.message : 'Save failed');
        } finally {
            setSaving(false);
        }
    };

    const onTest = async () => {
        setTesting(true);
        setMessage(null);
        setError(null);
        try {
            await rsocket.sendWebhookTest(machineId);
            setMessage('Test alert dispatched (check your sinks / server logs).');
        } catch (e: unknown) {
            setError(e instanceof Error ? e.message : 'Test dispatch failed');
        } finally {
            setTesting(false);
        }
    };

    if (loading) {
        return <div className="text-xs text-muted-foreground">Loading webhook settings…</div>;
    }

    return (
        <div className="space-y-4 rounded-lg border border-border/60 bg-card/30 p-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Outbound webhooks</h3>
                <Button type="button" size="sm" variant="secondary" onClick={() => void load()}>
                    Reload
                </Button>
            </div>
            <label className="flex items-center gap-2 text-xs text-foreground">
                <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
                Webhooks enabled
            </label>
            <div className="grid gap-3 sm:grid-cols-2">
                <label className="text-xs text-muted-foreground">
                    Dedupe cooldown (ms)
                    <input
                        type="number"
                        className="mt-1 w-full rounded border border-input bg-background px-2 py-1 text-xs font-mono"
                        value={dedupeMs}
                        min={0}
                        onChange={(e) => setDedupeMs(Number(e.target.value) || 0)}
                    />
                </label>
                <label className="text-xs text-muted-foreground">
                    HTTP timeout (seconds)
                    <input
                        type="number"
                        className="mt-1 w-full rounded border border-input bg-background px-2 py-1 text-xs font-mono"
                        value={timeoutSec}
                        min={1}
                        onChange={(e) => setTimeoutSec(Number(e.target.value) || 8)}
                    />
                </label>
            </div>
            <label className="block text-xs text-muted-foreground">
                Enabled alert kinds (comma-separated enum names)
                <input
                    className="mt-1 w-full rounded border border-input bg-background px-2 py-1 text-xs font-mono"
                    value={enabledKindsCsv}
                    onChange={(e) => setEnabledKindsCsv(e.target.value)}
                />
            </label>

            <div className="space-y-2">
                <div className="flex items-center justify-between">
                    <span className="text-[11px] font-medium text-foreground">Sinks</span>
                    <Button type="button" size="sm" variant="outline" onClick={() => setSinks((s) => [...s, newSinkDraft()])}>
                        Add sink
                    </Button>
                </div>
                {sinks.length === 0 ? (
                    <p className="text-[11px] italic text-muted-foreground">No sinks configured.</p>
                ) : (
                    sinks.map((s, idx) => (
                        <div key={s.id} className={cn('space-y-2 rounded border border-border/50 p-2', 'bg-background/50')}>
                            <div className="flex justify-between gap-2">
                                <span className="text-[10px] font-mono text-muted-foreground">Sink {idx + 1}</span>
                                <button
                                    type="button"
                                    className="text-[10px] text-destructive hover:underline"
                                    onClick={() => setSinks((rows) => rows.filter((r) => r.id !== s.id))}
                                >
                                    Remove
                                </button>
                            </div>
                            <label className="block text-[11px] text-muted-foreground">
                                Kind
                                <select
                                    className="mt-1 w-full rounded border border-input bg-background px-2 py-1 text-xs"
                                    value={s.kind}
                                    onChange={(e) =>
                                        setSinks((rows) =>
                                            rows.map((r) => (r.id === s.id ? { ...r, kind: e.target.value } : r))
                                        )
                                    }
                                >
                                    <option value="DISCORD">DISCORD</option>
                                    <option value="TELEGRAM">TELEGRAM</option>
                                    <option value="GENERIC_JSON">GENERIC_JSON</option>
                                </select>
                            </label>
                            <label className="block text-[11px] text-muted-foreground">
                                URL
                                <input
                                    className="mt-1 w-full rounded border border-input bg-background px-2 py-1 text-xs font-mono"
                                    value={s.url}
                                    onChange={(e) =>
                                        setSinks((rows) =>
                                            rows.map((r) => (r.id === s.id ? { ...r, url: e.target.value } : r))
                                        )
                                    }
                                />
                            </label>
                            <label className="block text-[11px] text-muted-foreground">
                                Auth token (e.g. Telegram chat_id)
                                <input
                                    className="mt-1 w-full rounded border border-input bg-background px-2 py-1 text-xs font-mono"
                                    value={s.authToken}
                                    onChange={(e) =>
                                        setSinks((rows) =>
                                            rows.map((r) => (r.id === s.id ? { ...r, authToken: e.target.value } : r))
                                        )
                                    }
                                />
                            </label>
                            <label className="block text-[11px] text-muted-foreground">
                                Per-sink kind filter (optional, comma-separated)
                                <input
                                    className="mt-1 w-full rounded border border-input bg-background px-2 py-1 text-xs font-mono"
                                    value={s.kindFilterCsv}
                                    onChange={(e) =>
                                        setSinks((rows) =>
                                            rows.map((r) => (r.id === s.id ? { ...r, kindFilterCsv: e.target.value } : r))
                                        )
                                    }
                                />
                            </label>
                        </div>
                    ))
                )}
            </div>

            <div className="flex flex-wrap gap-2">
                <Button type="button" size="sm" onClick={() => void onSave()} disabled={saving}>
                    {saving ? 'Saving…' : 'Save webhooks'}
                </Button>
                <Button type="button" size="sm" variant="outline" onClick={() => void onTest()} disabled={testing}>
                    {testing ? 'Sending…' : 'Send test alert'}
                </Button>
            </div>
            {message ? <p className="text-[11px] text-emerald-600 dark:text-emerald-400">{message}</p> : null}
            {error ? <p className="text-[11px] text-destructive">{error}</p> : null}
            <p className="text-[10px] text-muted-foreground">
                Telegram uses the sink URL as the Bot API endpoint and the auth token field as <code className="font-mono">chat_id</code>.
                Test alerts use kind CUSTOM and skip dedupe; they still require webhooks enabled and a valid sink URL.
            </p>
        </div>
    );
}
