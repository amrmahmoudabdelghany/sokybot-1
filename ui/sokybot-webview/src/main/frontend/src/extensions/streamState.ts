import { safeEval } from './renderer/expressionUtils';
import type { StreamBindingConfig, StreamMergeMode } from './ui-types';

type AnyState = Record<string, any>;

export interface StreamEnvelope {
    payload: any;
    binding: StreamBindingConfig;
    streamId: string;
}

function getCapacity(binding: StreamBindingConfig): number {
    return Math.max(1, Number(binding.maxCapacity ?? binding.rollingWindow ?? 1000));
}

function getControl(binding: StreamBindingConfig) {
    return {
        flushSignalType: binding.control?.flushSignalType || 'FLUSH',
        snapshotSignalType: binding.control?.snapshotSignalType || 'SNAPSHOT',
        deleteField: binding.control?.deleteField || '_delete',
    };
}

function extractRowKey(item: any, binding: StreamBindingConfig): string | null {
    if (!item || typeof item !== 'object') return null;
    const keyName = binding.rowKey || 'id';
    let value = item[keyName];
    if (value == null && binding.rowKeyExtractor) {
        try {
            value = safeEval(binding.rowKeyExtractor, item);
        } catch {
            value = undefined;
        }
    }
    if (value == null || value === '') return null;
    return String(value);
}

function truncateLargeRow(row: any, binding: StreamBindingConfig): any {
    const maxRowSize = Number(binding.maxRowSize || 0);
    if (!maxRowSize || !row || typeof row !== 'object') return row;
    try {
        const text = JSON.stringify(row);
        if (text.length <= maxRowSize) return row;
        return { ...row, _truncated: true, _rawSize: text.length };
    } catch {
        return row;
    }
}

function normalizeMode(binding: StreamBindingConfig): StreamMergeMode {
    return (binding.mode || 'replace') as StreamMergeMode;
}

function applyDelta(target: any[], value: any, binding: StreamBindingConfig): any[] {
    const rows = Array.isArray(target) ? target.slice() : [];
    const batch = Array.isArray(value) ? value : [value];
    const { deleteField } = getControl(binding);
    const capacity = getCapacity(binding);

    batch.forEach((entry) => {
        const key = extractRowKey(entry, binding);
        const isDelete = Boolean(entry && typeof entry === 'object' && entry[deleteField]);
        if (!key) {
            if (binding.orphanDeltaPolicy === 'upsert') {
                rows.push(truncateLargeRow(entry, binding));
            }
            return;
        }
        const idx = rows.findIndex((row) => extractRowKey(row, binding) === key);
        if (isDelete) {
            if (idx >= 0) rows.splice(idx, 1);
            return;
        }
        if (idx >= 0) {
            rows[idx] = truncateLargeRow({ ...rows[idx], ...entry }, binding);
            return;
        }
        if (binding.orphanDeltaPolicy === 'upsert') {
            rows.push(truncateLargeRow(entry, binding));
        }
    });

    return rows.length > capacity ? rows.slice(-capacity) : rows;
}

export function applyStreamBatch(prev: AnyState, envelopes: StreamEnvelope[]): AnyState {
    if (!envelopes.length) return prev;
    let next: AnyState = { ...prev };

    for (const envelope of envelopes) {
        const { binding, streamId } = envelope;
        const stateKey = binding.stateKey || streamId;
        const value = envelope.payload;
        const mode = normalizeMode(binding);
        const control = getControl(binding);
        const existing = next[stateKey];

        if (value && typeof value === 'object' && value.type === 'STATUS') {
            const streamHealth = next.streamHealth && typeof next.streamHealth === 'object' ? next.streamHealth : {};
            const normalizedStatus = {
                totalPacketsSeen: Number(value.totalPacketsSeen ?? 0),
                droppedIgnoredCount: Number(value.droppedIgnoredCount ?? 0),
                droppedFilterCount: Number(value.droppedFilterCount ?? 0),
                lastDropReason: String(value.lastDropReason ?? ''),
                lastDropTimestamp: Number(value.lastDropTimestamp ?? 0),
                lastDisplayedPacketTimestamp: Number(value.lastDisplayedPacketTimestamp ?? 0),
            };
            next.streamHealth = {
                ...streamHealth,
                [streamId]: {
                    ...(streamHealth[streamId] || {}),
                    ...value,
                    ...normalizedStatus,
                    status: value.bindingState || (value.isSubscribed ? 'running' : 'unsubscribed'),
                    updatedAt: Date.now(),
                }
            };
            // Mirror key diagnostics in top-level context for declarative components.
            next.totalPacketsSeen = normalizedStatus.totalPacketsSeen;
            next.droppedIgnoredCount = normalizedStatus.droppedIgnoredCount;
            next.droppedFilterCount = normalizedStatus.droppedFilterCount;
            next.lastDropReason = normalizedStatus.lastDropReason;
            next.lastDropTimestamp = normalizedStatus.lastDropTimestamp;
            next.lastDisplayedPacketTimestamp = normalizedStatus.lastDisplayedPacketTimestamp;
            continue;
        }

        if (value && typeof value === 'object' && value.type === control.flushSignalType) {
            next[stateKey] = [];
            continue;
        }
        if (value && typeof value === 'object' && value.type === control.snapshotSignalType) {
            next[stateKey] = Array.isArray(value.items) ? value.items.slice(-getCapacity(binding)) : [];
            continue;
        }

        if (binding.preFilterExpression) {
            try {
                const keep = safeEval(binding.preFilterExpression, { item: value, state: next });
                if (!keep) continue;
            } catch {
                // ignore invalid filter expression and keep payload
            }
        }

        if (mode === 'append') {
            const existingRows = Array.isArray(existing) ? existing : [];
            const currentGeneration = Number(next.streamHealth?.[streamId]?.subscriptionGeneration ?? -1);
            const incomingRows = (Array.isArray(value) ? value : [value])
                .filter((row) => {
                    if (!row || typeof row !== 'object') return true;
                    const generation = Number((row as any).subscriptionGeneration ?? currentGeneration);
                    return generation >= currentGeneration;
                })
                .map((row) => truncateLargeRow(row, binding));
            const merged = existingRows.concat(incomingRows);
            next[stateKey] = merged.slice(-getCapacity(binding));
        } else if (mode === 'delta') {
            next[stateKey] = applyDelta(Array.isArray(existing) ? existing : [], value, binding);
        } else if (mode === 'snapshot') {
            const rows = Array.isArray(value?.items) ? value.items : (Array.isArray(value) ? value : []);
            next[stateKey] = rows.slice(-getCapacity(binding));
        } else {
            next[stateKey] = value;
        }
    }

    return next;
}
