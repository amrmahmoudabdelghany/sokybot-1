import type { ComponentType } from 'react';
import { DeclarativeExtensionView } from './DeclarativeExtensionView';
import type { ExtensionComponent } from './ui-types';

/**
 * Component Registry - Maps component types to React components
 */
export const extensionComponentRegistry: Record<string, ExtensionComponent> = {
    // Declarative UI renderer
    'declarative': {
        component: DeclarativeExtensionView,
    },

    // Alias for convenience
    'custom': {
        component: DeclarativeExtensionView,
    },
};

/**
 * Register a new component type
 */
export function registerComponentType(
    type: string,
    component: ComponentType<any>,
    defaultProps?: Record<string, any>
) {
    extensionComponentRegistry[type] = { component, defaultProps };
}

/**
 * Get component by type
 */
export function getComponent(type: string): ExtensionComponent | undefined {
    return extensionComponentRegistry[type];
}
