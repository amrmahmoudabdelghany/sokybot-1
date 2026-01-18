import React, { useEffect, useState, useCallback, useMemo, useRef } from 'react';
import { rsocketService } from '../RSocketClient';
import { ComponentRenderer } from './renderer/ComponentRenderer';
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
    const updateTimeoutRef = useRef<NodeJS.Timeout>();
    // const lastUpdateRef = useRef<number>(0);
    // const pendingUpdatesRef = useRef<Map<string, any>>(new Map());
    const streamSubscriptionsRef = useRef<Map<string, any>>(new Map());

    // Performance: Memoize action handler
    const handleAction = useCallback(async (action: string, actionData: any) => {
        try {
            const response = await rsocketService.requestResponse(
                `extension.action:${pageId}:${action}:${JSON.stringify(actionData)}`
            );
            const result = typeof response === 'string' ? JSON.parse(response) : response;

            // Apply delta updates efficiently
            if (result.delta) {
                applyDeltaUpdate(result.delta);
            } else if (result.state) {
                setData(prev => ({ ...prev, ...result.state }));
            }

            return result;
        } catch (err) {
            console.error(`Action ${action} failed`, err);
            throw err;
        }
    }, [pageId]);

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

    // Auto-discover and subscribe to streams in schema
    useEffect(() => {
        if (!providedSchema) {
            fetchSchema();
        }

        // Discover streams in schema
        discoverAndSubscribeStreams(providedSchema || schema);

        // Subscribe to state change events (delta updates)
        const subscription = rsocketService.streamEvents(
            (event) => {
                if (event.type === `${pageId}.stateChanged` && event.delta) {
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
        };
    }, [pageId, machineId, providedSchema]);

    const fetchSchema = async () => {
        setLoading(true);
        try {
            const response = await rsocketService.requestResponse(
                `extension.schema:${pageId}${machineId ? `:${machineId}` : ''}`
            );
            const result = typeof response === 'string' ? JSON.parse(response) : response;

            if (result.schema) {
                setSchema(result.schema);
            }

            if (result.data || result.state) {
                setData(result.data || result.state || {});
            }
        } catch (err) {
            console.error(`Failed to fetch schema for ${pageId}`, err);
        } finally {
            setLoading(false);
        }
    };

    const discoverAndSubscribeStreams = (schema: any) => {
        if (!schema) return;

        // Check if component has stream
        if (schema.type === 'stream' || schema.props?.streamId) {
            const streamId = schema.props?.streamId || schema.id;
            const streamParams = schema.props?.streamParams || {};
            const stateKey = schema.props?.stateKey;

            if (streamId) {
                subscribeToStream(streamId, streamParams, stateKey);
            }
        }

        // Recursively check children
        if (schema.children && Array.isArray(schema.children)) {
            schema.children.forEach((child: any) => discoverAndSubscribeStreams(child));
        }
    };

    const subscribeToStream = (
        streamId: string,
        params: Record<string, any>,
        stateKey?: string
    ) => {
        const streamRequest = `extension.stream:${pageId}:${streamId}:${JSON.stringify(params)}`;

        const subscription = stateKey
            ? rsocketService.requestStreamWithState(
                streamRequest,
                stateKey,
                (state) => setData(prev => ({ ...prev, ...state }))
            )
            : rsocketService.requestStream(
                streamRequest,
                (streamData) => {
                    setData(prev => ({
                        ...prev,
                        [streamId]: streamData
                    }));
                }
            );

        streamSubscriptionsRef.current.set(streamId, subscription);
    };

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
