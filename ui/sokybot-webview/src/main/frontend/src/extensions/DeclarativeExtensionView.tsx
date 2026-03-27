import React, { useEffect, useState, useCallback, useMemo, useRef } from 'react';
import { rsocketService } from '../RSocketClient';
import { ComponentRenderer } from './renderer/ComponentRenderer';
import { resolveTemplateInObject, cleanResolvedParams } from './renderer/expressionUtils';
import type { UIComponent, StreamBindingConfig } from './ui-types';
import { validateSchema } from './schemaValidation';
import { applyStreamBatch, type StreamEnvelope } from './streamState';

interface DeclarativeExtensionViewProps {
    pageId: string;
    machineId?: string;
    schema?: UIComponent | UIComponent[];
}

/**
 * Declarative Extension View - Renders UI from JSON schemas
 */
export const DeclarativeExtensionView: React.FC<DeclarativeExtensionViewProps> = ({
    pageId,
    machineId,
    schema: providedSchema
}) => {
    const [schema, setSchema] = useState<UIComponent | UIComponent[] | null>(
        providedSchema || null
    );
    const [data, setData] = useState<Record<string, any>>({});
    const [loading, setLoading] = useState(!providedSchema);
    const [schemaIssues, setSchemaIssues] = useState<string[]>([]);

    const updateTimeoutRef = useRef<NodeJS.Timeout | undefined>(undefined);
    const streamSubscriptionsRef = useRef<Map<string, any>>(new Map());
    const lastStreamParamsRef = useRef<Map<string, string>>(new Map());
    const streamBindingsRef = useRef<Map<string, StreamBindingConfig>>(new Map());
    const streamQueuesRef = useRef<Map<string, StreamEnvelope[]>>(new Map());
    const flushIntervalRef = useRef<NodeJS.Timeout | undefined>(undefined);
    const streamVersionRef = useRef<Map<string, number>>(new Map());
    const streamConnectLockRef = useRef<Map<string, boolean>>(new Map());
    const streamConnectTimeoutRef = useRef<Map<string, NodeJS.Timeout>>(new Map());
    const streamLastActivityRef = useRef<Map<string, number>>(new Map());
    const streamStatusIntervalRef = useRef<NodeJS.Timeout | undefined>(undefined);

    const applyDeltaUpdate = useCallback((delta: Record<string, any>) => {
        setData(prev => ({ ...prev, ...delta }));
    }, []);

    const flushStreamQueues = useCallback(() => {
        const pending: StreamEnvelope[] = [];
        streamQueuesRef.current.forEach((items) => {
            if (items.length > 0) pending.push(...items.splice(0, items.length));
        });
        if (pending.length === 0) return;
        setData(prev => applyStreamBatch(prev, pending));
    }, []);

    const setStreamStatus = useCallback((streamId: string, status: string, details?: Record<string, any>) => {
        setData(prev => {
            const current = prev.streamHealth && typeof prev.streamHealth === 'object' ? prev.streamHealth : {};
            const previousStatus = current[streamId]?.status;
            const nextEntry = {
                ...(current[streamId] || {}),
                status,
                updatedAt: Date.now(),
                ...(details || {}),
            };
            const nextData: Record<string, any> = { ...prev, streamHealth: { ...current, [streamId]: nextEntry } };

            // Continuity marker: if stream recovers after being stalled/disconnected, annotate history.
            if (status === 'running' && (previousStatus === 'stalled' || previousStatus === 'disconnected' || previousStatus === 'timeout')) {
                const binding = streamBindingsRef.current.get(streamId);
                const stateKey = binding?.stateKey || streamId;
                const existingRows = Array.isArray(nextData[stateKey]) ? nextData[stateKey] : [];
                const marker = {
                    type: 'SYSTEM_MARKER',
                    markerType: 'connection_gap',
                    message: 'Connection interrupted - some packets may be missing',
                    timestamp: Date.now(),
                    source: 'SYSTEM',
                    name: 'Stream continuity marker',
                    opcode: '--',
                    count: '--',
                    crc: '--',
                    payload: ''
                };
                nextData[stateKey] = [...existingRows, marker];
            }
            return nextData;
        });
    }, []);

    const setTransportState = useCallback((state: 'connected' | 'disconnected') => {
        setData(prev => ({ ...prev, transportState: state, transportUpdatedAt: Date.now() }));
    }, []);

    // Performance: Memoize action handler
    const handleAction = useCallback(async (action: string, actionData: any) => {
        console.log(`[handleAction] action=${action}`, actionData);
        const toSafeHex = (value: unknown): string =>
            String(value ?? '')
                .replace(/[^A-Fa-f0-9]/g, '')
                .toUpperCase();

        const parseOpcodeLiteral = (value: unknown): string => {
            const text = String(value ?? '').trim();
            if (!text) return '0x0000';
            if (/^0x[0-9a-f]+$/i.test(text)) return `0x${text.slice(2).toUpperCase()}`;
            if (/^\d+$/.test(text)) return `0x${Number(text).toString(16).toUpperCase()}`;
            return `0x${text.replace(/^0x/i, '').toUpperCase()}`;
        };

        const toNetworkPeerLiteral = (source: unknown): string => {
            const normalized = String(source ?? '').toUpperCase();
            if (normalized === 'SERVER') return 'NetworkPeer.SERVER';
            if (normalized === 'CLIENT') return 'NetworkPeer.CLIENT';
            return 'NetworkPeer.BOT';
        };

        const toEncodingLiteral = (encoding: unknown): string => {
            const normalized = String(encoding ?? '').toUpperCase();
            if (normalized === 'ENCRYPTED') return 'Encoding.ENCRYPTED';
            return 'Encoding.PLAIN';
        };

        // Optimistic close: dismiss dialog immediately so it's always dismissible
        if (action === 'closeAnalyzer') {
            setData(prev => ({ ...prev, analyzerOpen: false }));
        }
        if (action === 'clearLog') {
            setData(prev => {
                const currentLog = prev.Log && typeof prev.Log === 'object' ? prev.Log : {};
                return {
                    ...prev,
                    Log: {
                        ...currentLog,
                        events: []
                    }
                };
            });
        }
        if (action === 'selectPackets') {
            const indices = Array.isArray(actionData?.indices) ? actionData.indices : [];
            setData(prev => ({ ...prev, selectedPacketIndices: indices }));
        }
        if (action === 'copyAsCode') {
            const payloadHex = toSafeHex(actionData?.payload);
            const opcodeLiteral = parseOpcodeLiteral(actionData?.opcode);
            const sourceLiteral = toNetworkPeerLiteral(actionData?.source);
            const encodingLiteral = toEncodingLiteral(actionData?.encoding);
            const byteLength = payloadHex.length > 0 ? payloadHex.length / 2 : Number(actionData?.size || 0);
            const directionMethod = String(actionData?.source ?? '').toUpperCase() === 'SERVER'
                ? 'sendToClient'
                : 'sendToServer';
            const snippet = [
                'def hexToBytes = { String hex ->',
                '    String clean = hex.replaceAll(/[^A-Fa-f0-9]/, "")',
                '    byte[] out = new byte[(int)(clean.length() / 2)]',
                '    for (int i = 0; i < clean.length(); i += 2) {',
                '        out[(int)(i / 2)] = (byte) Integer.parseInt(clean.substring(i, i + 2), 16)',
                '    }',
                '    out',
                '}',
                '',
                `def packet = MutablePacket.getBuilder(${byteLength}, ${opcodeLiteral})`,
                `    .packetEncoding(${encodingLiteral})`,
                '    .dataEncoding(Encoding.PLAIN)',
                `    .packetSource(${sourceLiteral})`,
                `    .putBytes(hexToBytes("${payloadHex}"))`,
                '    .build()',
                `context.getDispatcher().${directionMethod}(packet)`
            ].join('\n');
            const successful = await copyToClipboard(snippet);
            return { success: successful, copied: successful };
        }
        if (action === 'copyHex') {
            const payload = String(actionData?.payload ?? '');
            const normalized = payload.replace(/[^A-Fa-f0-9]/g, '').toUpperCase();
            const spacedHex = normalized.replace(/(..)(?=.)/g, '$1 ').trim();
            const success = await copyToClipboard(spacedHex);
            return { success, copied: success };
        }
        if (action === 'copyJson') {
            const json = JSON.stringify(actionData ?? {}, null, 2);
            const success = await copyToClipboard(json);
            return { success, copied: success };
        }

        try {
            const result = await rsocketService.request<any>('extension.action', {
                pageId,
                action,
                data: actionData
            });
            console.log(`[handleAction] result for ${action}:`, { success: result?.success, error: result?.error, hasDelta: !!result?.delta, hasState: result?.state != null, diffOpen: result?.state?.diffOpen });
            if (result == null) return result;

            // Apply delta updates efficiently
            if (result.delta) {
                applyDeltaUpdate(result.delta);
            }
            // Some handlers return state in `result.state`, others return plain fields directly.
            const hasPlainState =
                result &&
                typeof result === 'object' &&
                !Array.isArray(result) &&
                result.state == null &&
                Object.keys(result).some(key => !['success', 'error', 'delta'].includes(key));
            const state = result.state ?? (hasPlainState ? result : null);

            // Apply full state (for openAnalyzer ensure modal opens even if state shape differs)
            if (state != null || (action === 'openAnalyzer' && result.success !== false)) {
                setData(prev => {
                    const merged = state != null ? { ...prev, ...state } : { ...prev };
                    if (action === 'openAnalyzer' && result.success !== false) {
                        merged.analyzerOpen = true;
                        // Ensure analyzer data is never dropped (hex viewer + variables table)
                        if (Array.isArray(state?.packets)) merged.packets = state.packets;
                        if (Array.isArray(state?.variables)) merged.variables = state.variables;
                        if (state?.selectedByteCount != null) merged.selectedByteCount = state.selectedByteCount;
                        if (state?.matchCount != null) merged.matchCount = state.matchCount;
                    }
                    // Preserve trafficPackets if we have more than the action response (e.g. stream appended more)
                    const incoming = state?.trafficPackets;
                    const existing = prev.trafficPackets;
                    if (Array.isArray(existing) && Array.isArray(incoming) && existing.length > incoming.length) {
                        merged.trafficPackets = existing;
                    }
                    // Preserve selectedPacketIndices from optimistic frontend state when the
                    // server response returns a stale or empty list (e.g. due to async timing).
                    const prevSel = prev.selectedPacketIndices;
                    const incomingSel = merged.selectedPacketIndices;
                    if (Array.isArray(prevSel) && prevSel.length > 0 &&
                        (!Array.isArray(incomingSel) || incomingSel.length === 0) &&
                        action !== 'clearMonitor') {
                        console.log(`[handleAction] Preserving selection for ${action}: prev=${JSON.stringify(prevSel)}, incoming=${JSON.stringify(incomingSel)}`);
                        merged.selectedPacketIndices = prevSel;
                    } else if (Array.isArray(incomingSel) && incomingSel.length > 0) {
                        console.log(`[handleAction] Updating selection for ${action}: incoming=${JSON.stringify(incomingSel)}`);
                    }
                    return merged;
                });
            }

            return result;
        } catch (err) {
            console.error(`Action ${action} failed`, err);
            throw err;
        }
    }, [pageId, applyDeltaUpdate]);

    // Effect 1: Fetch schema if not provided
    useEffect(() => {
        if (!providedSchema) {
            fetchSchema();
        } else {
            const issues = validateSchema(providedSchema);
            setSchemaIssues(issues.map(i => `${i.path}: ${i.message}`));
        }
    }, [pageId, machineId, providedSchema]);

    // Effect 2: Manage subscriptions (Streams & Events)
    useEffect(() => {
        // Cleanup previous subscriptions
        streamSubscriptionsRef.current.forEach(sub => sub?.unsubscribe());
        streamSubscriptionsRef.current.clear();
        lastStreamParamsRef.current.clear();

        // Discover streams in schema
        discoverAndSubscribeStreams(providedSchema || schema);

        // Subscribe to state change events (delta updates)
        const subscription = rsocketService.subscribeToExtensionEvents(
            (event) => {
                const payload = event as any;
                if (payload.type === `${pageId}.stateChanged` && payload.delta) {
                    applyDeltaUpdate(payload.delta);
                }
            },
            (error) => console.error("Extension event error", error)
        );

        if (flushIntervalRef.current) clearInterval(flushIntervalRef.current);
        flushIntervalRef.current = setInterval(flushStreamQueues, 75);
        setTransportState('connected');
        if (streamStatusIntervalRef.current) clearInterval(streamStatusIntervalRef.current);
        streamStatusIntervalRef.current = setInterval(() => {
            const now = Date.now();
            const staleAfterMs = 15000;
            streamLastActivityRef.current.forEach((last, streamId) => {
                if (now - last >= staleAfterMs) {
                    setStreamStatus(streamId, 'stalled', { lastActivityAt: last, staleAfterMs });
                }
            });
        }, 1000);

        return () => {
            subscription?.unsubscribe();
            if (updateTimeoutRef.current) {
                clearTimeout(updateTimeoutRef.current);
            }
            // Cleanup streams
            streamSubscriptionsRef.current.forEach(sub => sub?.unsubscribe());
            streamSubscriptionsRef.current.clear();
            lastStreamParamsRef.current.clear();
            streamBindingsRef.current.clear();
            streamQueuesRef.current.clear();
            streamVersionRef.current.clear();
            if (flushIntervalRef.current) clearInterval(flushIntervalRef.current);
            if (streamStatusIntervalRef.current) clearInterval(streamStatusIntervalRef.current);
            streamConnectTimeoutRef.current.forEach(timeout => clearTimeout(timeout));
            streamConnectTimeoutRef.current.clear();
            streamConnectLockRef.current.clear();
            streamLastActivityRef.current.clear();
            setTransportState('disconnected');
        };
    }, [pageId, machineId, providedSchema, schema, applyDeltaUpdate, flushStreamQueues, setStreamStatus, setTransportState]);

    const fetchSchema = async () => {
        setLoading(true);
        console.log(`[${pageId}] Fetching schema for machine: ${machineId}`);
        try {
            const result = await rsocketService.request<any>('extension.schema', {
                pageId,
                machineId
            });

            if (result && result.schema) {
                console.log(`[${pageId}] Schema loaded successfully`);
                const issues = validateSchema(result.schema);
                if (issues.length > 0) {
                    console.warn(`[${pageId}] Schema validation issues:`, issues);
                }
                setSchemaIssues(issues.map(i => `${i.path}: ${i.message}`));
                setSchema(result.schema);
            } else {
                console.warn(`[${pageId}] No schema returned from backend for page: ${pageId}`);
            }

            if (result && (result.data || result.state)) {
                const newState = result.data || result.state || {};
                console.log(`[${pageId}] Initial state loaded:`, newState);
                setData(newState);
            } else {
                console.log(`[${pageId}] No initial state provided`);
            }
        } catch (err: any) {
            console.error(`[${pageId}] Failed to fetch schema:`, err);
            setSchemaIssues([`Network error: ${err.message || 'Unknown error'}`]);
        } finally {
            setLoading(false);
        }
    };

    const discoverAndSubscribeStreams = (schema: any) => {
        if (!schema) return;

        console.log(`[${pageId}] Scanning ${schema.type || 'Unknown'}:`, {
            hasProps: !!schema.props,
            streamId: schema.props?.streamId,
            id: schema.props?.id,
            props: schema.props
        });

        if (Array.isArray(schema)) {
            schema.forEach(child => discoverAndSubscribeStreams(child));
            return;
        }

        // Check if component has stream
        const binding: StreamBindingConfig | undefined = schema.stream || (schema.props?.streamId ? {
            streamId: schema.props.streamId,
            stateKey: schema.props.stateKey,
            mode: schema.props.streamMode || 'replace',
            streamParams: schema.props.streamParams || {},
            maxCapacity: schema.props.maxCapacity,
            rollingWindow: schema.props.rollingWindow,
            rowKey: schema.props.rowKey,
            rowKeyExtractor: schema.props.rowKeyExtractor,
            orphanDeltaPolicy: schema.props.orphanDeltaPolicy,
            maxQueueSize: schema.props.maxQueueSize,
            preFilterExpression: schema.props.preFilterExpression,
            pauseQueuePolicy: schema.props.pauseQueuePolicy,
            flushWhilePaused: schema.props.flushWhilePaused,
            initialSnapshot: schema.props.initialSnapshot,
            maxRowSize: schema.props.maxRowSize,
            control: schema.props.control,
        } : undefined);

        if (schema.type === 'stream' || binding) {
            const streamId = binding?.streamId || schema.props?.streamId || schema.id;
            const rawStreamParams = binding?.streamParams || {};

            if (streamId) {
                const resolvedParams = cleanResolvedParams(resolveTemplateInObject(rawStreamParams, data));
                const serialized = JSON.stringify(resolvedParams || {});
                lastStreamParamsRef.current.set(streamId, serialized);

                console.log(
                    `[${pageId}] Found stream: ${streamId} with mode: ${binding?.mode}`,
                    resolvedParams
                );
                if (binding) {
                    streamBindingsRef.current.set(streamId, binding);
                }
                subscribeToStream(streamId, resolvedParams, binding);
            }
        }

        // Recursively check children
        if (schema.children) {
            if (Array.isArray(schema.children)) {
                schema.children.forEach((child: any) => discoverAndSubscribeStreams(child));
            } else {
                discoverAndSubscribeStreams(schema.children);
            }
        }
    };

    const subscribeToStream = (
        streamId: string,
        params: Record<string, any>,
        binding?: StreamBindingConfig
    ) => {
        const maxQueueSize = Math.max(64, Number(binding?.maxQueueSize ?? 2000));
        const key = streamId;
        if (streamConnectLockRef.current.get(key)) {
            console.warn(`[${pageId}] Stream ${key} connect ignored: already connecting`);
            return;
        }
        streamConnectLockRef.current.set(key, true);
        setStreamStatus(key, 'connecting');
        const nextVersion = (streamVersionRef.current.get(key) || 0) + 1;
        streamVersionRef.current.set(key, nextVersion);
        const connectTimeout = setTimeout(() => {
            if (streamConnectLockRef.current.get(key)) {
                streamConnectLockRef.current.set(key, false);
                setStreamStatus(key, 'timeout', { message: 'Connection attempt timed out (10s)' });
            }
        }, 10000);
        streamConnectTimeoutRef.current.set(key, connectTimeout);

        const subscription = rsocketService.subscribe(
            `extension.stream:${pageId}:${streamId}`,
            (streamData: any) => {
                if (streamVersionRef.current.get(key) !== nextVersion) {
                    return;
                }
                streamLastActivityRef.current.set(key, Date.now());
                if (streamConnectLockRef.current.get(key)) {
                    streamConnectLockRef.current.set(key, false);
                    const timeout = streamConnectTimeoutRef.current.get(key);
                    if (timeout) clearTimeout(timeout);
                    streamConnectTimeoutRef.current.delete(key);
                }
                if (streamData?.type === 'STREAM_TERMINATED') {
                    setStreamStatus(key, 'disconnected', { reason: streamData?.reason || 'terminated' });
                    return;
                }
                if (streamData?.type === 'STATUS') {
                    const status = streamData?.bindingState || (streamData?.isSubscribed ? 'running' : 'unsubscribed');
                    setStreamStatus(key, status, {
                        ...streamData,
                        heartbeatAt: streamData?.timestamp || Date.now(),
                    });
                    return;
                }
                setStreamStatus(key, 'running', { lastPacketAt: Date.now() });
                const queue = streamQueuesRef.current.get(key) || [];
                queue.push({ payload: streamData, binding: binding || { streamId }, streamId });
                if (queue.length > maxQueueSize) {
                    queue.splice(0, queue.length - maxQueueSize);
                }
                streamQueuesRef.current.set(key, queue);
            },
            (error) => {
                streamConnectLockRef.current.set(key, false);
                const timeout = streamConnectTimeoutRef.current.get(key);
                if (timeout) clearTimeout(timeout);
                streamConnectTimeoutRef.current.delete(key);
                setStreamStatus(key, 'disconnected', { error: String(error?.message || error || 'stream error') });
                setTransportState('disconnected');
                console.error(`Stream ${streamId} error`, error);
            },
            { params: { ...params, pageId }, initialRequestN: 64, requestN: 64, maxInFlight: maxQueueSize }
        );

        streamSubscriptionsRef.current.set(streamId, subscription);
    };

    // Effect 3: Re-subscribe streams when their resolved parameters change
    useEffect(() => {
        const schemaToScan = providedSchema || schema;
        if (!schemaToScan) {
            return;
        }

        const visit = (node: any) => {
            if (!node) return;

            if (Array.isArray(node)) {
                node.forEach(child => visit(child));
                return;
            }

            const binding: StreamBindingConfig | undefined = node.stream || (node.props?.streamId ? {
                streamId: node.props.streamId,
                stateKey: node.props.stateKey,
                mode: node.props.streamMode || 'replace',
                streamParams: node.props.streamParams || {},
            } : undefined);
            if (node.type === 'stream' || binding) {
                const streamId = binding?.streamId || node.props?.streamId || node.id;
                const rawStreamParams = binding?.streamParams || {};

                if (streamId) {
                    const resolvedParams = cleanResolvedParams(resolveTemplateInObject(rawStreamParams, data));
                    const serialized = JSON.stringify(resolvedParams || {});
                    const previous = lastStreamParamsRef.current.get(streamId);

                    if (previous !== undefined && previous !== serialized) {
                        console.log(
                            `[${pageId}] Stream params changed for ${streamId}, re-subscribing`,
                            resolvedParams
                        );
                        const existing = streamSubscriptionsRef.current.get(streamId);
                        existing?.unsubscribe();
                        subscribeToStream(streamId, resolvedParams, binding);
                        lastStreamParamsRef.current.set(streamId, serialized);
                    } else if (previous === undefined) {
                        lastStreamParamsRef.current.set(streamId, serialized);
                    }
                }
            }

            if (node.children) {
                if (Array.isArray(node.children)) {
                    node.children.forEach((child: any) => visit(child));
                } else {
                    visit(node.children);
                }
            }
        };

        visit(schemaToScan);
    }, [data, pageId, providedSchema, schema]);

    // Performance: Memoize rendered components
    const renderedContent = useMemo(() => {
        if (!schema) return null;

        if (Array.isArray(schema)) {
            return schema.map((component, idx) => (
                <ComponentRenderer
                    key={`${component.key || 'component'}-${idx}`}
                    component={component}
                    pageId={pageId}
                    machineId={machineId}
                    context={data}
                    onAction={handleAction}
                />
            ));
        }

        return (
            <ComponentRenderer
                component={schema}
                pageId={pageId}
                machineId={machineId}
                context={data}
                onAction={handleAction}
            />
        );
    }, [schema, pageId, machineId, data, handleAction]);

    if (loading) {
        return <div className="text-center py-4">Loading...</div>;
    }

    if (schemaIssues.length > 0) {
        return (
            <div className="p-4 space-y-2">
                <div className="font-semibold text-red-600 dark:text-red-400">Invalid declarative schema</div>
                <div className="text-sm text-muted-foreground">Fix the issues below (showing up to 20).</div>
                <ul className="text-xs font-mono whitespace-pre-wrap break-words list-disc pl-6">
                    {schemaIssues.slice(0, 20).map((msg, idx) => (
                        <li key={idx}>{msg}</li>
                    ))}
                </ul>
            </div>
        );
    }

    if (!schema) {
        return <div>No schema available for {pageId}</div>;
    }

    return <div className="h-full min-h-0 overflow-auto">{renderedContent}</div>;
};

async function copyToClipboard(text: string): Promise<boolean> {
    if (!text) return false;
    try {
        if (navigator.clipboard) {
            await navigator.clipboard.writeText(text);
            return true;
        }
    } catch (err) {
        console.error('navigator.clipboard.writeText failed', err);
    }

    try {
        const textArea = document.createElement("textarea");
        textArea.value = text;
        textArea.style.position = "fixed";
        textArea.style.left = "-9999px";
        textArea.style.top = "-9999px";
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        const successful = document.execCommand('copy');
        document.body.removeChild(textArea);
        return successful;
    } catch (err) {
        console.error('Fallback copy failed', err);
        return false;
    }
}

