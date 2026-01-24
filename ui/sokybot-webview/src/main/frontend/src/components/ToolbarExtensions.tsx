import React, { useEffect, useState } from 'react';
import { rsocketService } from '../RSocketClient';
import { Button } from '@sokybot/frontend-shared';
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from '@sokybot/frontend-shared';
import * as LucideIcons from 'lucide-react';

interface ToolbarAction {
    actionId: string;
    title: string;
    iconName: string;
    modalSchema: any;
}

export const ToolbarExtensions: React.FC = () => {
    const [toolbarActions, setToolbarActions] = useState<ToolbarAction[]>([]);
    const [activeModal, setActiveModal] = useState<ToolbarAction | null>(null);
    const [modalState, setModalState] = useState<Record<string, any>>({});
    const [loading, setLoading] = useState(false);

    // Fetch extension registry on mount
    useEffect(() => {
        fetchRegistry();

        // Subscribe to extension events for dynamic updates using new typed API
        const subscription = rsocketService.subscribeToExtensionEvents((event) => {
            if (event.type === 'extension.toolbar.added' || event.type === 'extension.toolbar.removed') {
                fetchRegistry();
            }
        });

        return () => {
            subscription?.unsubscribe?.();
        };
    }, []);

    const fetchRegistry = async () => {
        try {
            // Use new typed API
            const data = await rsocketService.getExtensionRegistry();

            if (data.toolbarActions) {
                const actions = Object.values(data.toolbarActions) as ToolbarAction[];
                setToolbarActions(actions);
            }
        } catch (err) {
            console.error('Failed to fetch extension registry:', err);
        }
    };

    const handleOpenModal = async (action: ToolbarAction) => {
        setActiveModal(action);
        setModalState({});
        setLoading(true);

        try {
            // Trigger initial data load via action using new typed API
            const result = await rsocketService.triggerToolbarAction(action.actionId, 'init', {});
            if (result && (result as any).state) {
                setModalState((result as any).state);
            }
        } catch (err) {
            console.error('Failed to initialize modal:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleAction = async (actionName: string, data: Record<string, any> = {}) => {
        if (!activeModal) return;

        try {
            // Use new typed API
            const result = await rsocketService.triggerToolbarAction(activeModal.actionId, actionName, data);
            if (result && (result as any).state) {
                setModalState(prev => ({ ...prev, ...(result as any).state }));
            }
            return result;
        } catch (err) {
            console.error('Action failed:', err);
            throw err;
        }
    };

    const getIcon = (iconName: string): React.ComponentType<any> => {
        const Icon = (LucideIcons as any)[iconName];
        return Icon || LucideIcons.Box;
    };

    return (
        <>
            {/* Toolbar Buttons */}
            {toolbarActions.map(action => {
                const Icon = getIcon(action.iconName);
                return (
                    <Button
                        key={action.actionId}
                        variant="ghost"
                        size="icon"
                        title={action.title}
                        onClick={() => handleOpenModal(action)}
                    >
                        <Icon className="h-4 w-4" />
                    </Button>
                );
            })}

            {/* Modal Dialog */}
            <Dialog open={!!activeModal} onOpenChange={(open) => !open && setActiveModal(null)}>
                <DialogContent className="max-w-4xl max-h-[80vh] overflow-hidden flex flex-col">
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-2">
                            {activeModal && (
                                <>
                                    {React.createElement(getIcon(activeModal.iconName), { className: "h-5 w-5" })}
                                    {activeModal.title}
                                </>
                            )}
                        </DialogTitle>
                    </DialogHeader>
                    <div className="flex-1 overflow-auto">
                        {loading ? (
                            <div className="flex items-center justify-center py-8">
                                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
                            </div>
                        ) : activeModal ? (
                            <ModalContent
                                schema={activeModal.modalSchema}
                                state={modalState}
                                onAction={handleAction}
                            />
                        ) : null}
                    </div>
                </DialogContent>
            </Dialog>
        </>
    );
};

// Simple declarative modal content renderer
interface ModalContentProps {
    schema: any;
    state: Record<string, any>;
    onAction: (action: string, data?: Record<string, any>) => Promise<any>;
}

const ModalContent: React.FC<ModalContentProps> = ({ schema, state, onAction }) => {
    if (!schema) {
        return <div className="text-muted-foreground text-center py-4">No content available</div>;
    }

    // For now, render a simple table if state has 'bundles' array
    if (state.bundles && Array.isArray(state.bundles)) {
        return (
            <div className="space-y-4">
                <div className="flex justify-between items-center">
                    <span className="text-sm text-muted-foreground">
                        {state.bundles.length} bundles installed
                    </span>
                    <Button variant="outline" size="sm" onClick={() => onAction('refresh')}>
                        Refresh
                    </Button>
                </div>
                <div className="border rounded-md overflow-hidden">
                    <table className="w-full text-sm">
                        <thead className="bg-muted/50">
                            <tr>
                                <th className="text-left p-2 font-medium">ID</th>
                                <th className="text-left p-2 font-medium">Symbolic Name</th>
                                <th className="text-left p-2 font-medium">Version</th>
                                <th className="text-left p-2 font-medium">State</th>
                                <th className="text-right p-2 font-medium">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {state.bundles.map((bundle: any) => (
                                <tr key={bundle.id} className="border-t">
                                    <td className="p-2 font-mono text-xs">{bundle.id}</td>
                                    <td className="p-2">{bundle.symbolicName}</td>
                                    <td className="p-2 text-muted-foreground">{bundle.version}</td>
                                    <td className="p-2">
                                        <BundleStateBadge state={bundle.state} />
                                    </td>
                                    <td className="p-2 text-right space-x-1">
                                        {bundle.state !== 'ACTIVE' && (
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                onClick={() => onAction('start', { bundleId: bundle.id })}
                                            >
                                                Start
                                            </Button>
                                        )}
                                        {bundle.state === 'ACTIVE' && (
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                onClick={() => onAction('stop', { bundleId: bundle.id })}
                                            >
                                                Stop
                                            </Button>
                                        )}
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            onClick={() => onAction('restart', { bundleId: bundle.id })}
                                        >
                                            Restart
                                        </Button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    }

    // Fallback: show state as JSON
    return (
        <pre className="text-xs bg-muted p-4 rounded overflow-auto">
            {JSON.stringify(state, null, 2)}
        </pre>
    );
};

const BundleStateBadge: React.FC<{ state: string }> = ({ state }) => {
    const colors: Record<string, string> = {
        ACTIVE: 'bg-green-500/20 text-green-600 dark:text-green-400',
        RESOLVED: 'bg-yellow-500/20 text-yellow-600 dark:text-yellow-400',
        INSTALLED: 'bg-blue-500/20 text-blue-600 dark:text-blue-400',
        STARTING: 'bg-cyan-500/20 text-cyan-600 dark:text-cyan-400',
        STOPPING: 'bg-orange-500/20 text-orange-600 dark:text-orange-400',
        UNINSTALLED: 'bg-red-500/20 text-red-600 dark:text-red-400',
    };

    return (
        <span className={`px-2 py-0.5 rounded text-xs font-medium ${colors[state] || 'bg-muted'}`}>
            {state}
        </span>
    );
};
