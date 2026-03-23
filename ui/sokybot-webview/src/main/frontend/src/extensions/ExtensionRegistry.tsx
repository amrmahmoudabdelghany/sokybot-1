import { useSokybotStore } from '../store';

export interface ExtensionPage {
    pageId: string;
    title: string;
    iconPath?: string;
    schema?: any;
    componentType?: string;
    props?: Record<string, any>;
}

export interface ExtensionRegistry {
    pages: Record<string, ExtensionPage>;
    toolbarActions: Record<string, any>;
}

/**
 * Hook to manage extension registry
 */
export const useExtensionRegistry = () => {
    const extensionRegistry = useSokybotStore(state => state.extensionRegistry);
    return extensionRegistry as ExtensionRegistry;
};
