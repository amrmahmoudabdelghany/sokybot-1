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
        return (
            <div className="flex flex-1 min-h-0 min-w-0 flex-col">
                <DeclarativeExtensionView pageId={pageId} machineId={machineId} />
            </div>
        );
    }

    const Component = extension.component;
    const mergedProps = { ...extension.defaultProps, ...props };

    return (
        <div className="flex flex-1 min-h-0 min-w-0 flex-col">
            <Component pageId={pageId} machineId={machineId} {...mergedProps} />
        </div>
    );
};
