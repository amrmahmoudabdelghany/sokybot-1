import React from 'react';
import type { UIComponent } from '../ui-types';
import { getComponentFromLibrary, renderIcon } from '../componentLibrary';
import { cn } from '@sokybot/frontend-shared';
import { getComponent } from '../registry';
// Force Vite HMR on compiler error
import { HexViewer } from '../components/HexViewer';
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

    // Resolve template expressions
    const resolvedClassName = resolveTemplate(className || '', context);
    const resolvedProps = resolveTemplateInObject(props, context);
    const resolvedStyle = style ? resolveTemplateInObject(style, context) : undefined;

    // Handle onClick with action
    if (resolvedProps.onClick && typeof resolvedProps.onClick === 'string') {
        const action = resolvedProps.onClick;
        const actionData = resolvedProps.actionData || {};
        resolvedProps.onClick = () => {
            if (onAction) {
                onAction(action, actionData).catch(err => {
                    console.error(`Action ${action} failed:`, err);
                });
            }
        };
    }

    // Handle onChange with action
    if (resolvedProps.onChange && typeof resolvedProps.onChange === 'string') {
        const action = resolvedProps.onChange;
        resolvedProps.onChange = (e: any) => {
            const value = e.target?.value ?? e;
            if (onAction) {
                onAction(action, { value, ...resolvedProps.actionData }).catch(err => {
                    console.error(`Action ${action} failed:`, err);
                });
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
            const renderItem = resolvedProps.renderItem;
            const renderedItems = data.map((item: any, index: number) => {
                // Create a context with the item
                const itemContext = { ...context, item, index };
                // Recursively render the renderItem template
                if (typeof renderItem === 'object') {
                    return (
                        <ComponentRenderer
                            key={item.key || item.id || item.slot || index}
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
        const columns = props.columns || [];

        const renderCell = (col: any, row: any) => {
            const value = row[col.field] ?? '';
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

        return (
            <div className={cn('overflow-x-auto', resolvedClassName)} style={resolvedStyle}>
                <table className="min-w-full border-collapse border border-slate-300 dark:border-slate-700">
                    <thead>
                        <tr className="bg-slate-100 dark:bg-slate-800 text-slate-900 dark:text-slate-100">
                            {columns.map((col: any) => (
                                <th key={col.field || col.key} className={cn('border p-2 text-left', col.className)}>
                                    {col.label || col.field}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {Array.isArray(data) && data.map((row: any, idx: number) => (
                            <tr
                                key={idx}
                                className={props.rowClassName}
                                onClick={() => {
                                    if (props.onRowClick && onAction) {
                                        onAction(props.onRowClick, { ...row, index: idx });
                                    }
                                }}
                            >
                                {columns.map((col: any) => (
                                    <td
                                        key={col.field || col.key}
                                        className={cn('border p-2', col.className)}
                                        title={!col.truncate && col.title ? String(row[col.title] ?? '') : undefined}
                                    >
                                        {renderCell(col, row)}
                                    </td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        );
    }

    // Handle hex viewer component
    if (type === 'hex-viewer' || type === 'HexViewer') {
        const rawPackets = props.dataSource ? context[props.dataSource] : (props.packets || context.packets);
        const packets = Array.isArray(rawPackets) ? rawPackets : [];
        const selectedHex = context.selectedHex || props.selectedHex || '';
        const selectedByteCount = context.selectedByteCount || props.selectedByteCount || 0;
        const matchCount = context.matchCount || props.matchCount || 0;
        const matches = context.matches || props.matches || [];
        const groupLen = props.groupLen || context.groupLen || 16;

        return (
            <HexViewer
                packets={packets}
                selectedHex={selectedHex}
                selectedByteCount={selectedByteCount}
                matchCount={matchCount}
                matches={matches}
                groupLen={groupLen}
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
                        key={item.key || idx}
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
                key={child.key || idx}
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

