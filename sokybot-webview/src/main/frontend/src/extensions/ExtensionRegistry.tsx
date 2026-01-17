import { useEffect, useState } from 'react';
import { rsocketService } from '../RSocketClient';

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
        // Load initial registry
        rsocketService.requestResponse("extension.registry")
            .then(data => {
                const reg = typeof data === 'string' ? JSON.parse(data) : data;
                setRegistry(reg);
            })
            .catch(err => console.error("Failed to load extension registry", err));

        // Listen for extension events
        const subscription = rsocketService.streamEvents(
            (event) => {
                if (event.type === 'ui.extension' && event.data) {
                    const data = event.data;
                    if (data.type === 'extension.page.added') {
                        setRegistry(prev => ({
                            ...prev,
                            pages: {
                                ...prev.pages,
                                [data.pageId]: {
                                    pageId: data.pageId,
                                    title: data.title,
                                    iconPath: data.iconPath,
                                    schema: data.schema,
                                    componentType: 'declarative',
                                    props: {}
                                }
                            }
                        }));
                    } else if (data.type === 'extension.page.removed') {
                        setRegistry(prev => {
                            const newPages = { ...prev.pages };
                            delete newPages[data.pageId];
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
