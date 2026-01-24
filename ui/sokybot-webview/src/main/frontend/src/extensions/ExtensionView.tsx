import React from 'react';
import { getComponent } from './registry';
import { DeclarativeExtensionView } from './DeclarativeExtensionView';

interface ExtensionViewProps {
    pageId: string;
    componentType: string;
    machineId?: string;
    props?: Record<string, any>;
}

/**
 * Extension View Wrapper - Selects appropriate renderer based on component type
 */
export const ExtensionView: React.FC<ExtensionViewProps> = ({
    pageId,
    componentType,
    machineId,
    props = {}
}) => {
    const extension = getComponent(componentType);

    if (!extension) {
        // Default to declarative if type not found
        return (
            <DeclarativeExtensionView
                pageId={pageId}
                machineId={machineId}
            />
        );
    }

    const Component = extension.component;
    const mergedProps = { ...extension.defaultProps, ...props };

    return (
        <Component
            pageId={pageId}
            machineId={machineId}
            {...mergedProps}
        />
    );
};
