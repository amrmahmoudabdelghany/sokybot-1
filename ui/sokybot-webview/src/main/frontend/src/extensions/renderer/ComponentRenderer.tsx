import React from 'react';
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
import { getComponentFromLibrary, renderIcon } from '../componentLibrary';
import { cn } from '@sokybot/frontend-shared';
import { getComponent } from '../registry';
import { HexViewer } from '../components/HexViewer';

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
            // Try to evaluate as template expression
            if (hiddenValue.startsWith('${') && hiddenValue.endsWith('}')) {
                const expr = hiddenValue.slice(2, -1);
                const result = evaluateBooleanExpression(expr, context);
                shouldHide = result === true;
            } else {
                // Handle string literals 'true'/'false'
                shouldHide = hiddenValue === 'true';
            }
        }
        if (shouldHide) {
            return null;
        }
    }

    // Handle dataSource with renderItem pattern (for rendering arrays)
    if (resolvedProps.dataSource && resolvedProps.renderItem) {
        const data = context[resolvedProps.dataSource];
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
        const data = props.dataSource ? context[props.dataSource] : (props.data || []);
        const columns = props.columns || [];

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
                                    <td key={col.field || col.key} className={cn('border p-2', col.className)}>
                                        {row[col.field] || ''}
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
        const packets = props.dataSource ? context[props.dataSource] : (props.packets || context.packets || []);
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
            ? (typeof resolvedProps.children === 'string'
                ? resolveTemplate(resolvedProps.children, context)
                : resolvedProps.children)
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

    // Handle table component
    if (type === 'table') {
        const data = props.dataSource ? context[props.dataSource] : (props.data || []);
        const columns = props.columns || [];

        return (
            <div className={cn('overflow-x-auto', resolvedClassName)} style={resolvedStyle}>
                <table className="min-w-full border-collapse border border-slate-300 dark:border-slate-700">
                    <thead>
                        <tr className="bg-slate-100 dark:bg-slate-800">
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
                                    <td key={col.field || col.key} className={cn('border p-2', col.className)}>
                                        {row[col.field] || ''}
                                    </td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        );
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

/**
 * Resolve template expressions like ${variable} or ${obj.property}
 */
function resolveTemplate(template: string, ctx: Record<string, any>): string {
    if (!template || typeof template !== 'string') return template;

    return template.replace(/\$\{([^}]+)\}/g, (match, expr) => {
        try {
            // Simple variable access
            if (ctx[expr] !== undefined) {
                return String(ctx[expr]);
            }
            // Expression evaluation (limited for security)
            const value = evaluateExpression(expr, ctx);
            return value != null ? String(value) : match;
        } catch (e) {
            console.warn(`Template resolution failed for: ${expr}`, e);
            return match;
        }
    });
}

/**
 * Resolve template expressions in objects recursively
 */
function resolveTemplateInObject(obj: any, ctx: Record<string, any>): any {
    if (typeof obj === 'string') {
        return resolveTemplate(obj, ctx);
    } else if (Array.isArray(obj)) {
        return obj.map(item => resolveTemplateInObject(item, ctx));
    } else if (obj && typeof obj === 'object') {
        const resolved: any = {};
        for (const [key, value] of Object.entries(obj)) {
            resolved[key] = resolveTemplateInObject(value, ctx);
        }
        return resolved;
    }
    return obj;
}

/**
 * Simple expression evaluator (for security, only allow property access)
 */
function evaluateExpression(expr: string, ctx: Record<string, any>): any {
    const parts = expr.trim().split(/[.\[\]]/).filter(p => p);
    let value = ctx;
    for (const part of parts) {
        if (value && typeof value === 'object') {
            value = value[part];
        } else {
            return undefined;
        }
    }
    return value;
}

/**
 * Evaluate boolean expressions like "activeTab !== 'traffic'" or "show === false"
 */
function evaluateBooleanExpression(expr: string, ctx: Record<string, any>): boolean {
    const trimmed = expr.trim();

    // Helper to resolve values (literals or expressions)
    const resolveValue = (val: string) => {
        const trimmedVal = val.trim();
        if (trimmedVal === 'true') return true;
        if (trimmedVal === 'false') return false;
        if (trimmedVal === 'null') return null;
        if (trimmedVal === 'undefined') return undefined;
        // Try to parse as number
        const numVal = Number(trimmedVal);
        if (!isNaN(numVal) && trimmedVal === String(numVal)) return numVal;
        // String literals
        if ((trimmedVal.startsWith("'") && trimmedVal.endsWith("'")) ||
            (trimmedVal.startsWith('"') && trimmedVal.endsWith('"'))) {
            return trimmedVal.slice(1, -1);
        }
        // Try to evaluate as expression
        return evaluateExpression(trimmedVal, ctx);
    };

    // Handle !== operator (check before != to avoid false matches)
    if (trimmed.includes('!==')) {
        const [left, right] = trimmed.split('!==').map(s => s.trim());
        const leftValue = resolveValue(left);
        const rightValue = resolveValue(right);
        return leftValue !== rightValue;
    }

    // Handle === operator (check before == to avoid false matches)
    if (trimmed.includes('===')) {
        const [left, right] = trimmed.split('===').map(s => s.trim());
        const leftValue = resolveValue(left);
        const rightValue = resolveValue(right);
        return leftValue === rightValue;
    }

    // Handle != operator
    if (trimmed.includes('!=')) {
        const [left, right] = trimmed.split('!=').map(s => s.trim());
        const leftValue = resolveValue(left);
        const rightValue = resolveValue(right);
        return leftValue != rightValue;
    }

    // Handle == operator
    if (trimmed.includes('==')) {
        const [left, right] = trimmed.split('==').map(s => s.trim());
        const leftValue = resolveValue(left);
        const rightValue = resolveValue(right);
        return leftValue == rightValue;
    }

    // Simple boolean value - try to resolve as literal first
    if (trimmed === 'true') return true;
    if (trimmed === 'false') return false;

    // Otherwise evaluate as expression
    const value = evaluateExpression(trimmed, ctx);
    return Boolean(value);
}
