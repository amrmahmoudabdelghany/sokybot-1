import React, { useEffect, useState, useCallback, useMemo, useRef } from 'react';
import { rsocketService } from '../RSocketClient';
import { ComponentRenderer } from './renderer/ComponentRenderer';
import { resolveTemplateInObject, cleanResolvedParams } from './renderer/expressionUtils';
// UIComponent inlined to work around Vite serving ui-types.ts as empty
interface UIComponent {
    type: string;
    props?: Record<string, any>;
    children?: UIComponent[] | string;
    className?: string;
    style?: Record<string, any>;
    key?: string;
    icon?: string;
    iconProps?: Record<string, any>;
    variant?: string;
    size?: string;
}

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

    // Performance: Debounce state updates
    const updateTimeoutRef = useRef<NodeJS.Timeout | undefined>(undefined);
    // const lastUpdateRef = useRef<number>(0);
    // const pendingUpdatesRef = useRef<Map<string, any>>(new Map());
    const streamSubscriptionsRef = useRef<Map<string, any>>(new Map());
    const lastStreamParamsRef = useRef<Map<string, string>>(new Map());

    // Efficient delta update application
    const applyDeltaUpdate = useCallback((delta: Record<string, any>) => {
        setData(prev => {
            const updated = { ...prev };

            // Handle new packets (append only)
            if (delta.newPackets) {
                const existing = prev.trafficPackets || [];
                updated.trafficPackets = [...existing, ...delta.newPackets];
                // Keep only last 1000 packets in memory
                if (updated.trafficPackets.length > 1000) {
                    updated.trafficPackets = updated.trafficPackets.slice(-1000);
                }
            }

            // Merge other delta fields
            Object.keys(delta).forEach(key => {
                if (key !== 'newPackets') {
                    updated[key] = delta[key];
                }
            });

            return updated;
        });
    }, []);

    // Performance: Memoize action handler
    const handleAction = useCallback(async (action: string, actionData: any) => {
        try {
            const result = await rsocketService.request<any>('extension.action', {
                pageId,
                action,
                data: actionData
            });
            if (result == null) return result;

            // Apply delta updates efficiently
            if (result.delta) {
                applyDeltaUpdate(result.delta);
            }
            // Apply full state (for openAnalyzer ensure modal opens even if state shape differs)
            const state = result.state;
            if (state != null || (action === 'openAnalyzer' && result.success !== false)) {
                setData(prev => {
                    const merged = state != null ? { ...prev, ...state } : { ...prev };
                    if (action === 'openAnalyzer' && result.success !== false) {
                        merged.analyzerOpen = true;
                    }
                    // Preserve trafficPackets if we have more than the action response (e.g. stream appended more)
                    const incoming = state?.trafficPackets;
                    const existing = prev.trafficPackets;
                    if (Array.isArray(existing) && Array.isArray(incoming) && existing.length > incoming.length) {
                        merged.trafficPackets = existing;
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
        const subscription = rsocketService.streamEvents(
            (event) => {
                // console.log("Received extension event:", event);
                if (event.type === `${pageId}.stateChanged` && event.delta) {
                    // console.log(`[${pageId}] Applying delta update`, event.delta);
                    applyDeltaUpdate(event.delta);
                }
            },
            (error) => console.error("Extension event error", error)
        );

        return () => {
            subscription?.unsubscribe();
            if (updateTimeoutRef.current) {
                clearTimeout(updateTimeoutRef.current);
            }
            // Cleanup streams
            streamSubscriptionsRef.current.forEach(sub => sub?.unsubscribe());
            streamSubscriptionsRef.current.clear();
            lastStreamParamsRef.current.clear();
        };
    }, [pageId, machineId, providedSchema, schema]);

    const fetchSchema = async () => {
        setLoading(true);
        try {
            const result = await rsocketService.request<any>('extension.schema', {
                pageId,
                machineId
            });

            if (result.schema) {
                setSchema(result.schema);
            }

            if (result.data || result.state) {
                const newState = result.data || result.state || {};
                console.log(`[${pageId}] Initial state loaded:`, newState);
                setData(newState);
            }
        } catch (err) {
            console.error(`Failed to fetch schema for ${pageId}`, err);
        } finally {
            setLoading(false);
        }
    };

    const discoverAndSubscribeStreams = (schema: any) => {
        if (!schema) return;

        console.log(`[${pageId}] Scanning for streams in:`, schema.type || (Array.isArray(schema) ? 'Array' : 'Unknown'));

        if (Array.isArray(schema)) {
            schema.forEach(child => discoverAndSubscribeStreams(child));
            return;
        }

        // Check if component has stream
        if (schema.type === 'stream' || schema.props?.streamId) {
            const streamId = schema.props?.streamId || schema.id;
            const rawStreamParams = schema.props?.streamParams || {};
            const stateKey = schema.props?.stateKey;
            const streamMode = schema.props?.streamMode;

            if (streamId) {
                const resolvedParams = cleanResolvedParams(resolveTemplateInObject(rawStreamParams, data));
                const serialized = JSON.stringify(resolvedParams || {});
                lastStreamParamsRef.current.set(streamId, serialized);

                console.log(
                    `[${pageId}] Found stream: ${streamId} with mode: ${streamMode}`,
                    resolvedParams
                );
                subscribeToStream(streamId, resolvedParams, stateKey, streamMode);
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
        stateKey?: string,
        mode?: string
    ) => {
        // Use structured subscribe method
        // Backend expects: extension.stream:pageId:streamId
        const subscription = rsocketService.subscribe(
            `extension.stream:${pageId}:${streamId}`,
            (streamData: any) => {
                console.log(`[Stream:${streamId}] Received data`, streamData);
                if (stateKey) {
                    if (mode === 'append') {
                        setData(prev => {
                            const existing = Array.isArray(prev[stateKey]) ? prev[stateKey] : [];
                            // Keep last 1000 items
                            const updated = [...existing, streamData].slice(-1000);
                            return { ...prev, [stateKey]: updated };
                        });
                    } else {
                        setData(prev => ({ ...prev, [stateKey]: streamData }));
                    }
                } else {
                    setData(prev => ({ ...prev, [streamId]: streamData }));
                }
            },
            (error) => console.error(`Stream ${streamId} error`, error),
            { ...params, pageId }
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

            if (node.type === 'stream' || node.props?.streamId) {
                const streamId = node.props?.streamId || node.id;
                const rawStreamParams = node.props?.streamParams || {};
                const stateKey = node.props?.stateKey;
                const streamMode = node.props?.streamMode;

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
                        subscribeToStream(streamId, resolvedParams, stateKey, streamMode);
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
                    key={component.key || idx}
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

    if (!schema) {
        return <div>No schema available for {pageId}</div>;
    }

    return <div className="h-full overflow-auto">{renderedContent}</div>;
};
