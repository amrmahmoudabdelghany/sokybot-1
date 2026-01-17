import React, { useState, useMemo } from 'react';
import { useExtensionRegistry } from './extensions/ExtensionRegistry';
import { ExtensionView } from './extensions/ExtensionView';

interface MachineViewProps {
    machineId: string;
}

type Tab = string;

export const MachineView: React.FC<MachineViewProps> = ({ machineId }) => {
    const extensionRegistry = useExtensionRegistry();
    
    // Get all tabs for this machine from extension registry
    // Pages are registered as "inventory_{machineFullName}", "skills_{machineFullName}", etc.
    // or "log_{machineFullName}", "packetSniffer_{machineFullName}", etc.
    const allTabs = useMemo(() => {
        const machineTabs: Tab[] = [];
        const machineFullName = machineId; // Could be "group.machine" or just "machine"
        
        // Find all pages that match this machine
        // Match pages ending with machineFullName or containing machineFullName
        Object.keys(extensionRegistry.pages || {}).forEach(pageId => {
            // Match pages like "inventory_group.machine", "log_group.machine", etc.
            // or pages that end with machineFullName
            if (pageId.endsWith('_' + machineFullName) || 
                pageId.endsWith('.' + machineFullName) ||
                (pageId.includes('_') && pageId.split('_').pop() === machineFullName) ||
                (pageId.includes('.') && pageId.split('.').pop() === machineFullName)) {
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
    
    const [activeTab, setActiveTab] = useState<Tab>(allTabs[0] || '');
    
    // Update activeTab when tabs change
    React.useEffect(() => {
        if (allTabs.length > 0 && !allTabs.includes(activeTab)) {
            setActiveTab(allTabs[0]);
        }
    }, [allTabs, activeTab]);

    const renderTabContent = () => {
        const extensionPage = extensionRegistry.pages[activeTab];
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

        return <div className="text-center py-4 text-slate-500">No page available for: {activeTab}</div>;
    };

    return (
        <div className="space-y-4">
            {/* Tabs Navigation */}
            <div className="flex border-b border-slate-200 dark:border-slate-700 space-x-1 overflow-x-auto">
                {allTabs.map(tab => {
                    const extensionPage = extensionRegistry.pages[tab];
                    const label = extensionPage?.title || tab;
                    
                    return (
                        <button
                            key={tab}
                            onClick={() => setActiveTab(tab)}
                            className={`px-4 py-2 text-sm font-medium rounded-t-lg transition-colors capitalize whitespace-nowrap
                                ${activeTab === tab
                                    ? "bg-white dark:bg-slate-800 text-emerald-600 dark:text-emerald-400 border-t border-l border-r border-slate-200 dark:border-slate-700 -mb-px"
                                    : "text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-900"}`}
                        >
                            {label}
                        </button>
                    );
                })}
            </div>

            {/* Tab Content */}
            <div className="bg-white dark:bg-slate-900/50 rounded-b p-4 border border-slate-200 dark:border-slate-800 min-h-[400px]">
                {renderTabContent()}
            </div>
        </div>
    );
};
