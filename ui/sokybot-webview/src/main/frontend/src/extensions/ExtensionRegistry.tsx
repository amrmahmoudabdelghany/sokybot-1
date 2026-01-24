import { useEffect, useState } from 'react';
import { rsocketService } from '../RSocketClient';
import type { ExtensionEvent } from '../RSocketClient';

interface ExtensionPage {
    pageId: string;
    title: string;
    iconPath?: string;
    schema?: any;
    componentType?: string;
    props?: Record<string, any>;
}

interface ExtensionRegistry {
    pages: Record<string, ExtensionPage>;
    buttons: Record<string, any>;
}

/**
 * Hook to manage extension registry
 */
export const useExtensionRegistry = () => {
    const [registry, setRegistry] = useState<ExtensionRegistry>({ 
        pages: {}, 
        buttons: {} 
    });

    useEffect(() => {
        // Load initial registry using new typed API
        rsocketService.getExtensionRegistry()
            .then(data => {
                setRegistry({
                    pages: data.pages || {},
                    buttons: {}
                });
            })
            .catch(err => console.error("Failed to load extension registry", err));

        // Listen for extension events using new typed API
        const subscription = rsocketService.subscribeToExtensionEvents(
            (event: ExtensionEvent) => {
                if (event.type === 'ui.extension' && event.data) {
                    const data = event.data;
                    if (data.type === 'extension.page.added') {
                        setRegistry(prev => ({
                            ...prev,
                            pages: {
                                ...prev.pages,
                                [data.pageId as string]: {
                                    pageId: data.pageId as string,
                                    title: data.title as string,
                                    iconPath: data.iconPath as string,
                                    schema: data.schema,
                                    componentType: 'declarative',
                                    props: {}
                                }
                            }
                        }));
                    } else if (data.type === 'extension.page.removed') {
                        setRegistry(prev => {
                            const newPages = { ...prev.pages };
                            delete newPages[data.pageId as string];
                            return { ...prev, pages: newPages };
                        });
                    }
                }
            },
            (error) => console.error("Extension event error", error)
        );

        return () => subscription?.unsubscribe();
    }, []);

    return registry;
};
