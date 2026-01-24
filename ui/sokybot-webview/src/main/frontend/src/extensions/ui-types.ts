/**
 * Type definitions for declarative UI extension system
 */

export interface UIComponent {
    type: string;  // Component type (shadcn name, HTML tag, or custom type)
    props?: Record<string, any>;
    children?: UIComponent[] | string;  // Support text children
    className?: string;  // Tailwind classes
    style?: Record<string, any>;
    key?: string;

    // Special props for library integration
    icon?: string;  // Lucide icon name
    iconProps?: Record<string, any>;
    variant?: string;  // For shadcn components with variants
    size?: string;     // For shadcn components with sizes
}

export interface UIPage {
    pageId: string;
    title: string;
    iconPath?: string;
    schema: UIComponent | UIComponent[];  // Root component(s)
    dataSource?: string;  // RSocket endpoint for dynamic data
    actions?: Record<string, string>;  // Action handlers
}

export type ExtensionComponentType =
    | 'dataTable'
    | 'form'
    | 'custom'
    | 'declarative'
    | 'inventory'
    | 'skills'
    | 'training'
    | string;  // Allow custom types

export interface ExtensionComponent {
    component: React.ComponentType<any>;
    defaultProps?: Record<string, any>;
}
