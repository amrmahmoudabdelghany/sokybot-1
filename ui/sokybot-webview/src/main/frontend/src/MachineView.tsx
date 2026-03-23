import React, { useMemo } from 'react';
import { useExtensionRegistry } from './extensions/ExtensionRegistry';
import { ExtensionView } from './extensions/ExtensionView';
import { useSokybotStore } from './store';

interface MachineViewProps {
    machineId: string;
}

type Tab = string;

export const MachineView: React.FC<MachineViewProps> = ({ machineId }) => {
    const extensionRegistry = useExtensionRegistry();
    const { selectedPageId, setSelectedPageId } = useSokybotStore();

    // Get all tabs for this machine from extension registry
    // Pages are registered as "inventory_{machineFullName}", "skills_{machineFullName}", etc.
    // or "log_{machineFullName}", "packetSniffer_{machineFullName}", etc.
    const allTabs = useMemo(() => {
        const machineTabs: Tab[] = [];
        const machineFullName = machineId; // Could be "group.machine" or just "machine"

        // Find all pages that match this machine
        // Match pages ending with machineFullName or containing machineFullName
        const lowerMachineFullName = machineFullName.toLowerCase();
        Object.keys(extensionRegistry.pages || {}).forEach(pageId => {
            const lowerPageId = pageId.toLowerCase();
            // Packet Analyzer is shown only as a modal from Packet Sniffer, not as a separate tab
            if (lowerPageId.startsWith('packetanalyzer_')) return;

            // Match pages like "inventory_group.machine", "log_group.machine", etc.
            if (lowerPageId.endsWith('_' + lowerMachineFullName) ||
                lowerPageId.endsWith('.' + lowerMachineFullName) ||
                (lowerPageId.includes('_') && lowerPageId.split('_').pop() === lowerMachineFullName) ||
                (lowerPageId.includes('.') && lowerPageId.split('.').pop() === lowerMachineFullName)) {
                machineTabs.push(pageId);
            }
        });

        // Sort tabs: core pages first (inventory, skills, training, env, log), then others
        const corePageOrder = ['inventory', 'skills', 'training', 'environment', 'log'];
        machineTabs.sort((a, b) => {
            const aBase = a.split('_')[0].toLowerCase();
            const bBase = b.split('_')[0].toLowerCase();
            const aIndex = corePageOrder.indexOf(aBase);
            const bIndex = corePageOrder.indexOf(bBase);
            if (aIndex !== -1 && bIndex !== -1) return aIndex - bIndex;
            if (aIndex !== -1) return -1;
            if (bIndex !== -1) return 1;
            return a.localeCompare(b);
        });

        return machineTabs;
    }, [extensionRegistry.pages, machineId]);

    // Sync default page
    React.useEffect(() => {
        if (allTabs.length > 0) {
            // If nothing selected, or selected page not in this machine's tabs, select first
            if (!selectedPageId || !allTabs.includes(selectedPageId)) {
                setSelectedPageId(allTabs[0]);
            }
        }
    }, [allTabs, selectedPageId, setSelectedPageId]);

    const renderTabContent = () => {
        if (!selectedPageId) return null;

        const extensionPage = extensionRegistry.pages[selectedPageId];
        if (extensionPage) {
            return (
                <ExtensionView
                    pageId={extensionPage.pageId}
                    componentType={extensionPage.componentType || 'declarative'}
                    machineId={machineId}
                    props={extensionPage.props}
                />
            );
        }

        return <div className="text-center py-4 text-slate-500">No page available for: {selectedPageId}</div>;
    };

    return (
        <div className="h-full flex flex-col">
            {/* Tab Content - now full content area */}
            <div className="flex-1 bg-card overflow-auto">
                {renderTabContent()}
            </div>
        </div>
    );
};
