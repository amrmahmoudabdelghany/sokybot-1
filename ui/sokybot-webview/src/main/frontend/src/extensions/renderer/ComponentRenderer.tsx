import React, { useEffect, useMemo, useRef, useState } from 'react';
import type { UIComponent } from '../ui-types';
import { getComponentFromLibrary, renderIcon } from '../componentLibrary';
import { cn } from '@sokybot/frontend-shared';
import { getComponent } from '../registry';
// Force Vite HMR on compiler error
import { HexViewer } from '../components/HexViewer';
import { DiffHexViewer } from '../components/DiffHexViewer';
import { LogViewer } from '../components/LogViewer';
import { StreamTablePanel } from '../components/StreamTablePanel';
import { safeEval, resolveTemplate, resolveTemplateInObject } from './expressionUtils';

interface ComponentRendererProps {
    component: UIComponent;
    pageId: string;
    machineId?: string;
    context?: Record<string, any>;  // State/data context
    onAction?: (action: string, data: any) => Promise<any>;
}

/**
 * Component Renderer - Dynamically renders UI components from declarative schemas
 */
export const ComponentRenderer: React.FC<ComponentRendererProps> = ({
    component,
    pageId,
    machineId,
    context = {},
    onAction
}) => {
    const { type, props = {}, children, style, className } = component;
    const debounceTimersRef = useRef<Record<string, ReturnType<typeof setTimeout>>>({});
    const [localInputDraft, setLocalInputDraft] = useState<any>(undefined);

    useEffect(() => {
        return () => {
            Object.values(debounceTimersRef.current).forEach((timer) => clearTimeout(timer));
            debounceTimersRef.current = {};
        };
    }, []);

    const handleActionResult = (result: any) => {
        if (!result || typeof window === 'undefined') return;
        if (result.hexDump && typeof result.hexDump === 'string') {
            const filename = result.filename || 'packets.hex';
            const blob = new Blob([result.hexDump], { type: 'text/plain;charset=utf-8' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            a.click();
            URL.revokeObjectURL(url);
        }
        if (result.binaryBase64 && typeof result.binaryBase64 === 'string') {
            const bytes = Uint8Array.from(atob(result.binaryBase64), c => c.charCodeAt(0));
            const blob = new Blob([bytes], { type: 'application/octet-stream' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = (result.filename || 'packets.bin').replace(/\.hex$/i, '.bin');
            a.click();
            URL.revokeObjectURL(url);
        }
    };

    // Resolve template expressions
    const resolvedClassName = resolveTemplate(className || '', context);
    const resolvedProps = resolveTemplateInObject(props, context);
    const resolvedStyle = style ? resolveTemplateInObject(style, context) : undefined;
    const isTextInputLike = type === 'Input' || type === 'input' || type === 'TextArea' || type === 'textarea';
    const serverValueSnapshot = resolvedProps.value;

    if (isTextInputLike && localInputDraft !== undefined) {
        resolvedProps.value = localInputDraft;
    }

    if (isTextInputLike && localInputDraft === undefined) {
        const value = resolvedProps.value;
        resolvedProps.value = value == null ? '' : String(value);
    }

    useEffect(() => {
        if (!isTextInputLike) return;
        if (localInputDraft === undefined) return;
        if (serverValueSnapshot === undefined || serverValueSnapshot === null) return;
        if (serverValueSnapshot === localInputDraft) {
            setLocalInputDraft(undefined);
        }
    }, [isTextInputLike, localInputDraft, serverValueSnapshot]);

    // Handle onClick with action
    if (resolvedProps.onClick && typeof resolvedProps.onClick === 'string') {
        const action = resolvedProps.onClick;
        const actionData = resolvedProps.actionData || {};
        resolvedProps.onClick = () => {
            if (onAction) {
                onAction(action, actionData).catch(err => {
                    console.error(`Action ${action} failed:`, err);
                }).then((result) => handleActionResult(result));
            }
        };
    }

    // Handle onChange with action
    if (resolvedProps.onChange && typeof resolvedProps.onChange === 'string') {
        const action = resolvedProps.onChange;
        const fieldName = typeof resolvedProps.name === 'string' ? resolvedProps.name : undefined;
        const supportsDebounce = type === 'Input' || type === 'input' || type === 'TextArea' || type === 'textarea';
        const debounceMs = Number(resolvedProps.debounceMs || 300);
        resolvedProps.onChange = (e: any) => {
            const value = e.target?.value ?? e;
            if (supportsDebounce && typeof value === 'string') {
                setLocalInputDraft(value);
            }
            const payload = {
                ...(fieldName ? { [fieldName]: value } : {}),
                value,
                ...resolvedProps.actionData
            };
            const invoke = () => {
                if (onAction) {
                    onAction(action, payload).catch(err => {
                        console.error(`Action ${action} failed:`, err);
                    });
                }
            };
            if (onAction) {
                const shouldDebounce = supportsDebounce && typeof value === 'string' && debounceMs > 0;
                if (shouldDebounce) {
                    const debounceKey = `${action}:${fieldName || 'value'}`;
                    const existing = debounceTimersRef.current[debounceKey];
                    if (existing) clearTimeout(existing);
                    debounceTimersRef.current[debounceKey] = setTimeout(() => {
                        delete debounceTimersRef.current[debounceKey];
                        invoke();
                    }, debounceMs);
                } else {
                    invoke();
                }
            }
        };
    }

    // Handle onBlur with action
    if (resolvedProps.onBlur && typeof resolvedProps.onBlur === 'string') {
        const action = resolvedProps.onBlur;
        resolvedProps.onBlur = (e: any) => {
            const value = e.target?.value ?? e;
            if (onAction) {
                onAction(action, { value, ...resolvedProps.actionData }).catch(err => {
                    console.error(`Action ${action} failed:`, err);
                });
            }
        };
    }

    // Handle onCheckedChange with action (e.g. Checkbox)
    if (resolvedProps.onCheckedChange && typeof resolvedProps.onCheckedChange === 'string') {
        const action = resolvedProps.onCheckedChange;
        const fieldName = typeof resolvedProps.name === 'string' ? resolvedProps.name : undefined;
        resolvedProps.onCheckedChange = (checked: any) => {
            const value = checked === 'indeterminate' ? false : Boolean(checked);
            if (onAction) {
                onAction(action, {
                    ...(fieldName ? { [fieldName]: value } : {}),
                    value,
                    ...resolvedProps.actionData
                }).catch(err => {
                    console.error(`Action ${action} failed:`, err);
                    // Keep UI source-of-truth server-driven; failed updates are reverted on next state sync.
                });
            }
        };
    }

    // Handle onSubmit with action (e.g. Form)
    if (resolvedProps.onSubmit && typeof resolvedProps.onSubmit === 'string') {
        const action = resolvedProps.onSubmit;
        resolvedProps.onSubmit = (e: React.FormEvent) => {
            e.preventDefault();
            if (onAction) {
                onAction(action, resolvedProps.actionData || {}).catch(err => {
                    console.error(`Action ${action} failed:`, err);
                });
            }
        };
    }

    // Handle icon prop
    if (resolvedProps.icon) {
        const iconElement = renderIcon(resolvedProps.icon, resolvedProps.iconProps || {});
        if (iconElement) {
            const existingChildren = resolvedProps.children
                ? (Array.isArray(resolvedProps.children) ? resolvedProps.children : [resolvedProps.children])
                : [];
            resolvedProps.children = [iconElement, ...existingChildren];
        }
    }

    // Handle hidden prop (support template expressions)
    const hiddenValue = resolvedProps.hidden;
    if (hiddenValue !== undefined && hiddenValue !== null) {
        let shouldHide = false;
        if (typeof hiddenValue === 'boolean') {
            shouldHide = hiddenValue;
        } else if (typeof hiddenValue === 'string') {
            if (hiddenValue.startsWith('${') && hiddenValue.endsWith('}')) {
                shouldHide = Boolean(safeEval(hiddenValue.slice(2, -1), context));
            } else {
                shouldHide = hiddenValue === 'true';
            }
        }
        if (shouldHide) {
            return null;
        }
    }

    // Coerce disabled prop to boolean (templates resolve to string "true"/"false")
    if (resolvedProps.disabled !== undefined) {
        const d = resolvedProps.disabled;
        if (typeof d === 'string') {
            resolvedProps.disabled = d === 'true' || (d !== 'false' && d !== '');
        } else {
            resolvedProps.disabled = Boolean(d);
        }
    }

    // Dialog: coerce open to boolean; when onOpenChange is an action name, call it when dialog closes
    if (type === 'Dialog') {
        if (resolvedProps.open !== undefined) {
            const o = resolvedProps.open;
            resolvedProps.open = typeof o === 'string' ? o === 'true' : Boolean(o);
        }
        if (typeof resolvedProps.onOpenChange === 'string') {
            const actionName = resolvedProps.onOpenChange;
            resolvedProps.onOpenChange = (open: boolean) => {
                if (!open && onAction) onAction(actionName, {});
            };
        }
    }

    // EmptyState: bind action (string) to onActionClick so the empty-state button triggers the action
    if (type === 'EmptyState' && typeof resolvedProps.action === 'string') {
        const actionName = resolvedProps.action;
        const actionData = resolvedProps.actionData || {};
        resolvedProps.onActionClick = () => {
            if (onAction) onAction(actionName, actionData).catch((err: unknown) => console.error(`Action ${actionName} failed:`, err));
        };
    }

    // Handle dataSource with renderItem pattern (for rendering arrays)
    if (resolvedProps.dataSource && resolvedProps.renderItem) {
        const data = safeEval(resolvedProps.dataSource, context);
        if (Array.isArray(data)) {
            // Keep the original template object so item-scoped expressions
            // are resolved only inside each child renderer with item context.
            const renderItem = props.renderItem;
            const renderedItems = data.map((item: any, index: number) => {
                // Create a context with the item
                const itemContext = { ...context, item, index };
                // Recursively render the renderItem template
                if (typeof renderItem === 'object') {
                    return (
                        <ComponentRenderer
                            key={`${item.key || item.id || item.slot || 'item'}-${index}`}
                            component={renderItem}
                            pageId={pageId}
                            machineId={machineId}
                            context={itemContext}
                            onAction={onAction}
                        />
                    );
                }
                return null;
            });

            const LibraryComponent = getComponentFromLibrary(type);
            if (LibraryComponent) {
                const { dataSource, renderItem, hidden, children: propsChildren, ...componentProps } = resolvedProps;
                if (typeof LibraryComponent === 'string') {
                    return React.createElement(
                        LibraryComponent,
                        {
                            className: resolvedClassName ? cn(resolvedClassName) : undefined,
                            style: resolvedStyle,
                            ...componentProps,
                        },
                        renderedItems
                    );
                } else {
                    return React.createElement(
                        LibraryComponent,
                        {
                            className: resolvedClassName ? cn(resolvedClassName) : undefined,
                            style: resolvedStyle,
                            ...componentProps,
                        },
                        renderedItems
                    );
                }
            }
        }
    }

    // Handle table component
    if (type === 'table') {
        const rawData = props.dataSource ? context[props.dataSource] : props.data;
        const data = Array.isArray(rawData) ? rawData : [];
        return (
            <TableRenderer
                data={data}
                props={props}
                resolvedClassName={resolvedClassName}
                resolvedStyle={resolvedStyle}
                context={context}
                onAction={onAction}
                handleActionResult={handleActionResult}
            />
        );
    }

    if (type === 'stream-table-panel') {
        const binding = component.stream || resolvedProps.stream || props.stream;
        const stateKey = binding?.stateKey || binding?.streamId;
        const rawData = stateKey ? context[stateKey] : [];
        const tableData = Array.isArray(rawData) ? rawData : [];
        return (
            <div className={cn('w-full h-full min-h-0 flex flex-col', resolvedClassName)} style={resolvedStyle}>
                <StreamTablePanel
                    binding={binding || { streamId: 'unknown', stateKey: 'unknown' }}
                    context={context}
                    onAction={onAction}
                    title={resolvedProps.title || 'Stream Table'}
                    filterKey={resolvedProps.filterKey}
                    filterAction={resolvedProps.filterAction}
                />
                <div className="flex-1 min-h-0 flex flex-col">
                    <TableRenderer
                        data={tableData}
                        props={resolvedProps.table || {}}
                        resolvedClassName="w-full h-full min-h-0"
                        resolvedStyle={undefined}
                        context={context}
                        onAction={onAction}
                        handleActionResult={handleActionResult}
                    />
                </div>
            </div>
        );
    }

    // Handle hex viewer component
    if (type === 'hex-viewer' || type === 'HexViewer') {
        const rawPackets = props.dataSource ? context[props.dataSource] : (props.packets || context.packets);
        let packets = Array.isArray(rawPackets) ? rawPackets : [];
        if (packets.length === 0 && Array.isArray(context.trafficPackets)) {
            packets = convertTrafficRowsToHexRows(context.trafficPackets);
        }
        const selectedHex = context.selectedHex || props.selectedHex || '';
        const selectedByteCount = context.selectedByteCount || props.selectedByteCount || 0;
        const matchCount = context.matchCount || props.matchCount || 0;
        const matches = context.matches || props.matches || [];
        const groupLen = props.groupLen || context.groupLen || 16;
        const structDefinitions = context.structDefinitions || props.structDefinitions || [];

        return (
            <HexViewer
                packets={packets}
                selectedHex={selectedHex}
                selectedByteCount={selectedByteCount}
                matchCount={matchCount}
                matches={matches}
                groupLen={groupLen}
                structDefinitions={structDefinitions}
                onSelectHex={(hex, startOffset, endOffset) => {
                    if (onAction && props.onSelectHex) {
                        onAction(props.onSelectHex, { hex, startOffset, endOffset });
                    }
                }}
                onDefineVariable={(hex, packetName) => {
                    if (onAction && props.onDefineVariable) {
                        onAction(props.onDefineVariable, { hex, packetName });
                    }
                }}
                onDefineField={(fieldData) => {
                    if (onAction && props.onDefineField) {
                        onAction(props.onDefineField, fieldData);
                    }
                }}
                editable={Boolean(props.editable)}
                defaultDirection={props.defaultDirection || 'C2S'}
                onInjectPacket={(payload) => {
                    if (onAction && props.onInjectPacket) {
                        onAction(props.onInjectPacket, payload).then(handleActionResult);
                    }
                }}
                className={resolvedClassName}
                style={resolvedStyle}
            />
        );
    }

    if (type === 'diff-hex-viewer' || type === 'DiffHexViewer') {
        const packetsA = Array.isArray(context[props.dataSourceA]) ? context[props.dataSourceA] : [];
        const packetsB = Array.isArray(context[props.dataSourceB]) ? context[props.dataSourceB] : [];
        return <DiffHexViewer packetsA={packetsA} packetsB={packetsB} className={resolvedClassName} style={resolvedStyle} />;
    }

    if (type === 'log-viewer' || type === 'LogViewer') {
        const source = resolvedProps.dataSource || props.dataSource;
        const rawEvents = typeof source === 'string' ? safeEval(source, context) : source;
        const events = Array.isArray(rawEvents) ? rawEvents : [];
        return (
            <LogViewer
                events={events}
                className={resolvedClassName}
                style={resolvedStyle}
            />
        );
    }

    // Try to get component from UI library first
    const LibraryComponent = getComponentFromLibrary(type);

    if (LibraryComponent) {
        // Determine children: prefer props.children, then component.children
        const resolvedChildren = resolvedProps.children !== undefined
            ? resolvePropsChildren(resolvedProps.children, pageId, machineId, context, onAction)
            : (children ? renderChildren(children, pageId, machineId, context, onAction) : undefined);

        // Handle string elements (HTML tags)
        if (typeof LibraryComponent === 'string') {
            // Remove hidden from props for HTML elements
            const { hidden, children: propsChildren, ...htmlProps } = resolvedProps;
            return React.createElement(
                LibraryComponent,
                {
                    className: resolvedClassName ? cn(resolvedClassName) : undefined,
                    style: resolvedStyle,
                    ...htmlProps,
                },
                resolvedChildren
            );
        }

        // Handle React components (shadcn/ui, etc.)
        // Remove hidden and children from props (we'll pass children separately)
        const { hidden, children: propsChildren, ...componentProps } = resolvedProps;
        return React.createElement(
            LibraryComponent,
            {
                className: resolvedClassName ? cn(resolvedClassName) : undefined,
                style: resolvedStyle,
                ...componentProps,
            },
            resolvedChildren
        );
    }

    // Handle built-in layout components
    if (type === 'layout') {
        const layoutType = props.layout || 'column';
        const gap = props.gap || 4;
        const padding = props.padding || 4;
        const cols = props.cols; // Assuming cols might be passed in props

        const layoutClass = ({
            row: 'flex flex-row',
            col: 'flex flex-col',
            grid: `grid ${cols ? `grid-cols-${cols}` : 'grid-cols-2'} gap-${gap}`,
            stack: `space-y-${gap}`
        } as Record<string, string>)[layoutType] || 'flex flex-col';

        return (
            <div
                className={cn(layoutClass, `gap-${gap} p-${padding}`, resolvedClassName)}
                style={resolvedStyle}
            >
                {renderChildren(children, pageId, machineId, context, onAction)}
            </div>
        );
    }

    // Handle special declarative components
    if (type === 'icon') {
        return renderIcon(props.name || 'Circle', {
            className: resolvedClassName,
            style: resolvedStyle,
            size: props.size || 24,
            ...resolvedProps
        }) || <span>Icon not found</span>;
    }

    // Try extension registry
    const registered = getComponent(type);
    if (registered) {
        const Component = registered.component;
        return (
            <Component
                {...resolvedProps}
                pageId={pageId}
                machineId={machineId}
                className={resolvedClassName}
                style={resolvedStyle}
            >
                {renderChildren(children, pageId, machineId, context, onAction)}
            </Component>
        );
    }

    // Fallback
    console.warn(`Unknown component type: ${type}`);
    return (
        <div className="text-red-500 p-2 border border-red-300 rounded">
            Unknown component type: {type}
        </div>
    );
};

function convertTrafficRowsToHexRows(rows: any[]): any[] {
    const result: any[] = [];
    rows.forEach((row) => {
        const source = String(row?.source ?? 'UNKNOWN');
        const name = String(row?.name ?? 'UNKNOWN');
        const opcode = String(row?.opcode ?? '0x0000');
        const payload = String(row?.payload ?? '').replace(/[^A-Fa-f0-9]/g, '').toUpperCase();
        if (!payload) return;

        result.push({ type: 'header', source, name, opcode });
        for (let i = 0; i < payload.length; i += 32) {
            const chunk = payload.slice(i, i + 32);
            const bytes = chunk.match(/.{1,2}/g) || [];
            const ascii = bytes
                .map((b) => {
                    const code = parseInt(b, 16);
                    return code >= 32 && code <= 126 ? String.fromCharCode(code) : '.';
                })
                .join('');
            result.push({
                type: 'data',
                lineNumber: i.toString(16).toUpperCase().padStart(6, '0'),
                hex: bytes.join(' '),
                ascii,
                startOffset: i / 2,
                endOffset: i / 2 + bytes.length,
            });
        }
        result.push({ type: 'separator' });
    });
    return result;
}

interface TableRendererProps {
    data: any[];
    props: Record<string, any>;
    resolvedClassName?: string;
    resolvedStyle?: React.CSSProperties;
    context: Record<string, any>;
    onAction?: (action: string, data: any) => Promise<any>;
    handleActionResult: (result: any) => void;
}

const TableRenderer: React.FC<TableRendererProps> = ({
    data,
    props,
    resolvedClassName,
    resolvedStyle,
    context,
    onAction,
    handleActionResult
}) => {
    const columns = props.columns || [];
    const rowKeyField = String(props.rowKey || 'index');
    const selectedIndices = Array.isArray(context.selectedPacketIndices) ? context.selectedPacketIndices : [];
    const selectedKeys = Array.isArray(context.selectedRowKeys) ? context.selectedRowKeys.map((k: any) => String(k)) : [];
    const [localSelectedIndices, setLocalSelectedIndices] = useState<number[]>(selectedIndices);
    const [localSelectedKeys, setLocalSelectedKeys] = useState<string[]>(selectedKeys);
    const selectionRef = useRef<number[]>(localSelectedIndices);
    const selectionKeyRef = useRef<string[]>(localSelectedKeys);
    selectionRef.current = localSelectedIndices;
    selectionKeyRef.current = localSelectedKeys;
    const expandable = Boolean(props.expandable);
    const autoScrollEnabled = Boolean(props.autoScroll);
    const rowConditionalStyle = props.rowConditionalStyle;
    const scrollContainerRef = useRef<HTMLDivElement | null>(null);
    const rowRefs = useRef<Array<HTMLTableRowElement | null>>([]);
    const [isPinnedToBottom, setIsPinnedToBottom] = useState(true);
    const [showJumpToLatest, setShowJumpToLatest] = useState(false);
    const [expandedRowIndex, setExpandedRowIndex] = useState<number | null>(null);
    const [focusedRowIndex, setFocusedRowIndex] = useState<number>(-1);

    const renderCell = (col: any, row: any) => {
        let value = row[col.field] ?? '';
        if (col.field === 'time' && !value && row.timestamp) {
            const d = new Date(row.timestamp);
            value = d.toLocaleTimeString('en-GB', {
                hour12: false,
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                fractionalSecondDigits: 3
            });
        }
        if (col.truncate && col.maxWidth) {
            return (
                <div
                    style={{ maxWidth: col.maxWidth, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                    title={col.title ? String(row[col.title] ?? '') : String(value)}
                >
                    {value}
                </div>
            );
        }
        return value;
    };

    const getRowConditionalStyle = useMemo(() => {
        if (!rowConditionalStyle || typeof rowConditionalStyle !== 'object') {
            return (_row: any) => undefined;
        }
        const field = rowConditionalStyle.field;
        const values = rowConditionalStyle.values;
        if (!field || !values || typeof values !== 'object') {
            return (_row: any) => undefined;
        }
        return (row: any) => {
            const rawValue = row?.[field];
            const key = rawValue == null ? '' : String(rawValue);
            const style = values[key];
            if (style && typeof style === 'object') {
                return style as React.CSSProperties;
            }
            return undefined;
        };
    }, [rowConditionalStyle]);

    useEffect(() => {
        if (!autoScrollEnabled || !isPinnedToBottom) {
            return;
        }
        const container = scrollContainerRef.current;
        if (!container) {
            return;
        }
        container.scrollTop = container.scrollHeight;
    }, [autoScrollEnabled, data.length, isPinnedToBottom]);

    useEffect(() => {
        if (!Array.isArray(data) || data.length === 0) {
            setFocusedRowIndex(-1);
            setExpandedRowIndex(prev => (prev == null ? prev : null));
            return;
        }
        if (focusedRowIndex >= data.length) {
            setFocusedRowIndex(data.length - 1);
        }
        if (expandedRowIndex != null && expandedRowIndex >= data.length) {
            setExpandedRowIndex(null);
        }
    }, [data, focusedRowIndex, expandedRowIndex]);

    const handleScroll = () => {
        if (!autoScrollEnabled) {
            return;
        }
        const container = scrollContainerRef.current;
        if (!container) {
            return;
        }
        const nearBottom = container.scrollTop + container.clientHeight >= container.scrollHeight - 24;
        setIsPinnedToBottom(nearBottom);
        setShowJumpToLatest(!nearBottom);
    };

    const jumpToLatest = () => {
        const container = scrollContainerRef.current;
        if (!container) {
            return;
        }
        container.scrollTop = container.scrollHeight;
        setIsPinnedToBottom(true);
        setShowJumpToLatest(false);
    };

    const payloadToHexRows = (payload: unknown) => {
        const normalized = String(payload ?? '').replace(/[^A-Fa-f0-9]/g, '').toUpperCase();
        if (!normalized) return [];
        const rows: Array<{ offset: string; hex: string; ascii: string }> = [];
        for (let i = 0; i < normalized.length; i += 32) {
            const chunk = normalized.slice(i, i + 32);
            const bytes = chunk.match(/.{1,2}/g) || [];
            const ascii = bytes
                .map((byte) => {
                    const code = parseInt(byte, 16);
                    return code >= 32 && code <= 126 ? String.fromCharCode(code) : '.';
                })
                .join('');
            rows.push({
                offset: i.toString(16).toUpperCase().padStart(6, '0'),
                hex: bytes.join(' '),
                ascii
            });
        }
        return rows;
    };

    return (
        <div className={cn('relative', resolvedClassName)} style={resolvedStyle}>
            <div
                ref={scrollContainerRef}
                className="h-full min-h-0 overflow-x-auto overflow-y-auto"
                tabIndex={0}
                onScroll={handleScroll}
                onKeyDown={(event) => {
                    if (!Array.isArray(data) || data.length === 0) {
                        return;
                    }
                    if (event.key === 'ArrowDown') {
                        event.preventDefault();
                        const next = Math.min(data.length - 1, focusedRowIndex + 1);
                        setFocusedRowIndex(next);
                        rowRefs.current[next]?.scrollIntoView({ block: 'nearest' });
                        return;
                    }
                    if (event.key === 'ArrowUp') {
                        event.preventDefault();
                        const next = focusedRowIndex < 0 ? 0 : Math.max(0, focusedRowIndex - 1);
                        setFocusedRowIndex(next);
                        rowRefs.current[next]?.scrollIntoView({ block: 'nearest' });
                        return;
                    }
                    if (event.key === 'Enter' && expandable && focusedRowIndex >= 0) {
                        event.preventDefault();
                        setExpandedRowIndex(prev => (prev === focusedRowIndex ? null : focusedRowIndex));
                        return;
                    }
                    if (event.key === 'Escape') {
                        event.preventDefault();
                        setExpandedRowIndex(null);
                        setFocusedRowIndex(-1);
                        return;
                    }
                    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'c' && focusedRowIndex >= 0) {
                        event.preventDefault();
                        navigator.clipboard.writeText(String(data[focusedRowIndex]?.payload ?? '')).catch(() => undefined);
                    }
                }}
            >
                <table className="min-w-full border-collapse border border-slate-300 dark:border-slate-700">
                    <thead>
                        <tr className="bg-slate-100 dark:bg-slate-800 text-slate-900 dark:text-slate-100">
                            {columns.map((col: any) => (
                                <th key={col.field || col.key} className={cn('border p-2 text-left', col.className)}>
                                    {col.label || col.field}
                                </th>
                            ))}
                            {Array.isArray(props.rowActions) && props.rowActions.length > 0 && (
                                <th className="border p-2 text-left">Actions</th>
                            )}
                        </tr>
                    </thead>
                    <tbody>
                        {Array.isArray(data) && data.map((row: any, idx: number) => {
                            const rowIndex = row.index ?? idx;
                            const isSystemMarker = row?.type === 'SYSTEM_MARKER';
                            const isExpanded = expandedRowIndex === rowIndex;
                            const rowKeyValue = String(row?.[rowKeyField] ?? rowIndex);
                            const isSelected = localSelectedKeys.includes(rowKeyValue) || localSelectedIndices.includes(rowIndex);
                            const hexRows = isExpanded ? payloadToHexRows(row.payload) : [];
                            const decodedFields = Array.isArray(row.decodedFields) ? row.decodedFields : [];
                            const colspan = columns.length + (Array.isArray(props.rowActions) && props.rowActions.length > 0 ? 1 : 0);
                            return (
                                <React.Fragment key={`${rowIndex}-${idx}`}>
                                    <tr
                                        ref={(el) => {
                                            rowRefs.current[rowIndex] = el;
                                        }}
                                        className={cn(props.rowClassName)}
                                        style={{
                                            ...getRowConditionalStyle(row),
                                            ...(isSelected
                                                ? {
                                                    // Inline selected styling so it always wins over rowConditionalStyle backgrounds.
                                                    backgroundColor: 'rgba(14, 165, 233, 0.24)',
                                                    boxShadow: 'inset 0 0 0 1px rgba(14, 165, 233, 0.55)'
                                                }
                                                : {}),
                                            ...(focusedRowIndex === rowIndex
                                                ? { outline: '2px solid rgb(56 189 248 / 0.9)', outlineOffset: '-2px' }
                                                : {})
                                        }}
                                        onClick={(e) => {
                                            if (isSystemMarker) return;
                                            const rowIndex = row.index ?? idx;
                                            setFocusedRowIndex(rowIndex);
                                            if (expandable && !e.ctrlKey && !e.metaKey) {
                                                setExpandedRowIndex(prev => (prev === rowIndex ? null : rowIndex));
                                            }
                                            if (props.onRowClick && onAction) {
                                                onAction(props.onRowClick, { ...row, index: rowIndex });
                                            }
                                            if (props.multiSelectAction && onAction) {
                                                const baseSelection = selectionRef.current;
                                                const baseKeySelection = selectionKeyRef.current;
                                                let next: number[];
                                                let nextKeys: string[];
                                                if (e.ctrlKey || e.metaKey) {
                                                    next = baseSelection.includes(rowIndex)
                                                        ? baseSelection.filter((value: number) => value !== rowIndex)
                                                        : [...baseSelection, rowIndex];
                                                    nextKeys = baseKeySelection.includes(rowKeyValue)
                                                        ? baseKeySelection.filter((value: string) => value !== rowKeyValue)
                                                        : [...baseKeySelection, rowKeyValue];
                                                } else {
                                                    if (baseSelection.length === 0) {
                                                        next = [rowIndex];
                                                        nextKeys = [rowKeyValue];
                                                    } else if (baseSelection.length === 1 && baseSelection[0] !== rowIndex) {
                                                        next = [baseSelection[0], rowIndex];
                                                        nextKeys = [baseKeySelection[0] || String(baseSelection[0]), rowKeyValue];
                                                    } else if (baseSelection.length > 1 && !baseSelection.includes(rowIndex)) {
                                                        next = [baseSelection[0], rowIndex];
                                                        nextKeys = [baseKeySelection[0] || String(baseSelection[0]), rowKeyValue];
                                                    } else {
                                                        next = [rowIndex];
                                                        nextKeys = [rowKeyValue];
                                                    }
                                                }
                                                console.log('[table] multiSelect:', { baseSelection, next, rowIndex });
                                                setLocalSelectedIndices(next);
                                                setLocalSelectedKeys(nextKeys);
                                                selectionRef.current = next;
                                                selectionKeyRef.current = nextKeys;
                                                onAction(props.multiSelectAction, { indices: next, rowKeys: nextKeys }).catch(err => {
                                                    console.error(`Action ${props.multiSelectAction} failed:`, err);
                                                });
                                            }
                                        }}
                                    >
                                        {columns.map((col: any) => (
                                            <td
                                                key={col.field || col.key}
                                                className={cn('border p-2', col.className)}
                                                title={!col.truncate && col.title ? String(row[col.title] ?? '') : undefined}
                                                onDoubleClick={() => {
                                                    if (props.onCellClick && onAction) {
                                                        onAction(props.onCellClick, {
                                                            index: idx,
                                                            field: col.field,
                                                            value: row[col.field]
                                                        }).catch(err => console.error(`Action ${props.onCellClick} failed:`, err));
                                                    }
                                                }}
                                            >
                                                {renderCell(col, row)}
                                            </td>
                                        ))}
                                        {Array.isArray(props.rowActions) && props.rowActions.length > 0 && (
                                            <td className="border p-1 whitespace-nowrap">
                                                <div className="flex gap-1 justify-center">
                                                    {props.rowActions.map((actionCfg: any, actionIdx: number) => (
                                                        <button
                                                            key={`${idx}-${actionIdx}`}
                                                            type="button"
                                                            className="px-1.5 py-0.5 text-xs border border-border rounded hover:bg-muted/80 transition-colors"
                                                            title={actionCfg.title || actionCfg.label || 'Action'}
                                                            disabled={isSystemMarker}
                                                            onClick={(e) => {
                                                                e.stopPropagation();
                                                                if (isSystemMarker) return;
                                                                if (onAction && actionCfg.action) {
                                                                    const liveRow = data.find((candidate: any) => String(candidate?.[rowKeyField] ?? '') === rowKeyValue);
                                                                    if (!liveRow) {
                                                                        console.warn(`Skipping row action ${actionCfg.action}: row key evicted`, rowKeyValue);
                                                                        return;
                                                                    }
                                                                    onAction(actionCfg.action, { ...row, index: idx, ...(actionCfg.data || {}) })
                                                                        .then(handleActionResult)
                                                                        .catch(err => console.error(`Action ${actionCfg.action} failed:`, err));
                                                                }
                                                            }}
                                                        >
                                                            {actionCfg.label || 'Action'}
                                                        </button>
                                                    ))}
                                                </div>
                                            </td>
                                        )}
                                    </tr>
                                    {expandable && isExpanded && (
                                        <tr>
                                            <td className="border p-0" colSpan={colspan}>
                                                <div className="p-3 bg-muted/20 space-y-3">
                                                    <div className="flex items-center justify-between">
                                                        <div className="text-xs font-semibold text-muted-foreground uppercase">Packet Details</div>
                                                        <button
                                                            type="button"
                                                            className="px-2 py-1 text-xs border border-border rounded hover:bg-muted"
                                                            onClick={(e) => {
                                                                e.stopPropagation();
                                                                navigator.clipboard.writeText(String(row.payload ?? '')).catch(() => undefined);
                                                            }}
                                                        >
                                                            Copy Hex
                                                        </button>
                                                    </div>
                                                    <div className="overflow-x-auto border border-border rounded">
                                                        <table className="min-w-full text-xs font-mono">
                                                            <thead>
                                                                <tr className="bg-muted/40">
                                                                    <th className="p-2 text-left">Offset</th>
                                                                    <th className="p-2 text-left">Hex</th>
                                                                    <th className="p-2 text-left">ASCII</th>
                                                                </tr>
                                                            </thead>
                                                            <tbody>
                                                                {hexRows.map((line, lineIdx) => (
                                                                    <tr key={`${line.offset}-${lineIdx}`} className="border-t border-border/50">
                                                                        <td className="p-2">{line.offset}</td>
                                                                        <td className="p-2">{line.hex}</td>
                                                                        <td className="p-2">{line.ascii}</td>
                                                                    </tr>
                                                                ))}
                                                            </tbody>
                                                        </table>
                                                    </div>
                                                    {decodedFields.length > 0 && (
                                                        <div className="overflow-x-auto border border-border rounded">
                                                            <table className="min-w-full text-xs">
                                                                <thead>
                                                                    <tr className="bg-muted/40">
                                                                        <th className="p-2 text-left">Field</th>
                                                                        <th className="p-2 text-left">Type</th>
                                                                        <th className="p-2 text-left">Hex</th>
                                                                        <th className="p-2 text-left">Value</th>
                                                                    </tr>
                                                                </thead>
                                                                <tbody>
                                                                    {decodedFields.map((field: any, fieldIdx: number) => (
                                                                        <tr key={`${field.name || 'field'}-${fieldIdx}`} className="border-t border-border/50">
                                                                            <td className="p-2">{field.name || '-'}</td>
                                                                            <td className="p-2">{field.type || '-'}</td>
                                                                            <td className="p-2 font-mono">{field.hexValue || '-'}</td>
                                                                            <td className="p-2">{String(field.decodedValue ?? '-')}</td>
                                                                        </tr>
                                                                    ))}
                                                                </tbody>
                                                            </table>
                                                        </div>
                                                    )}
                                                </div>
                                            </td>
                                        </tr>
                                    )}
                                </React.Fragment>
                            );
                        })}
                    </tbody>
                </table>
            </div>
            {autoScrollEnabled && showJumpToLatest && (
                <button
                    type="button"
                    className="absolute bottom-3 right-3 z-10 rounded border border-border bg-background/95 px-2 py-1 text-xs text-foreground shadow hover:bg-muted"
                    onClick={jumpToLatest}
                >
                    Jump to latest
                </button>
            )}
        </div>
    );
};

/**
 * Resolve props.children which may be a string, an array of UIComponent objects, or a mixed array
 */
function resolvePropsChildren(
    value: any,
    pageId: string,
    machineId: string | undefined,
    context: Record<string, any>,
    onAction?: (action: string, data: any) => Promise<any>
): React.ReactNode {
    if (typeof value === 'string') {
        return resolveTemplate(value, context);
    }
    if (Array.isArray(value)) {
        return value.map((item: any, idx: number) => {
            if (typeof item === 'string') {
                return resolveTemplate(item, context);
            }
            if (React.isValidElement(item)) {
                return item;
            }
            if (item && typeof item === 'object' && item.type) {
                return (
                    <ComponentRenderer
                        key={`${item.key || 'item'}-${idx}`}
                        component={item}
                        pageId={pageId}
                        machineId={machineId}
                        context={context}
                        onAction={onAction}
                    />
                );
            }
            return item;
        });
    }
    if (React.isValidElement(value)) {
        return value;
    }
    if (value && typeof value === 'object' && value.type) {
        return (
            <ComponentRenderer
                component={value}
                pageId={pageId}
                machineId={machineId}
                context={context}
                onAction={onAction}
            />
        );
    }
    return value;
}

/**
 * Render children components
 */
function renderChildren(
    children: UIComponent[] | string | undefined,
    pageId: string,
    machineId: string | undefined,
    context: Record<string, any>,
    onAction?: (action: string, data: any) => Promise<any>
): React.ReactNode {
    if (!children) return null;

    if (typeof children === 'string') {
        return resolveTemplate(children, context);
    }

    if (Array.isArray(children)) {
        return children.map((child, idx) => (
            <ComponentRenderer
                key={`${child.key || 'child'}-${idx}`}
                component={child}
                pageId={pageId}
                machineId={machineId}
                context={context}
                onAction={onAction}
            />
        ));
    }

    return undefined;
}

