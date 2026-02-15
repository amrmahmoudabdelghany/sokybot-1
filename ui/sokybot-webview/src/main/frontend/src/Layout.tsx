import React, { useState } from 'react';
import { useSokybotStore } from './store';
import { useExtensionRegistry } from './extensions/ExtensionRegistry';
import { CharacterStatus } from './CharacterStatus';
import { useTheme } from './components/theme-provider';
import { CreateGroupDialog } from './components/CreateGroupDialog';
import CreateMachineDialog from './components/CreateMachineDialog';
import { ToolbarExtensions } from './components/ToolbarExtensions';
import { Plus, Bot, Monitor, Moon, Sun, Maximize, Minimize, Settings, Target, Package, Zap, Globe, FileText, Activity } from 'lucide-react';
import { Button } from '@sokybot/frontend-shared';
import { cn } from '@sokybot/frontend-shared';

interface LayoutProps {
    children: React.ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
    const {
        groups,
        machines,
        selectedMachineId,
        setSelectedMachineId,
        fetchInitialData
    } = useSokybotStore();

    const extensionRegistry = useExtensionRegistry();
    const { selectedPageId, setSelectedPageId } = useSokybotStore();

    // Helper to get machine simple name
    const getSimpleName = (machineId: string) => {
        return machineId.includes('.') ? machineId.split('.').pop() || machineId : machineId;
    };

    // Helper to get machine group
    const getMachineGroup = (machineId: string) => {
        return machineId.includes('.') ? machineId.split('.')[0] : '';
    };

    // Helper to get machine tabs (duplicated from MachineView for now)
    const getMachineTabs = (machineId: string) => {
        const machineTabs: string[] = [];
        const lowerMachineId = machineId.toLowerCase();

        Object.keys(extensionRegistry.pages || {}).forEach(pageId => {
            const lowerPageId = pageId.toLowerCase();

            // Strict match: pageId must end with exactly _machineId
            // This prevents "TEST" from matching "TESTA" and prevents "ASD" from matching group names
            if (lowerPageId === lowerMachineId ||
                lowerPageId.endsWith("_" + lowerMachineId)) {
                machineTabs.push(pageId);
            }
        });

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
    };

    const { theme, setTheme } = useTheme();
    const [isCreateGroupOpen, setCreateGroupOpen] = useState(false);
    const [isCreateMachineOpen, setCreateMachineOpen] = useState(false);
    const [isFullscreen, setIsFullscreen] = useState(false);

    const toggleFullscreen = () => {
        if (!document.fullscreenElement) {
            document.documentElement.requestFullscreen();
            setIsFullscreen(true);
        } else {
            document.exitFullscreen();
            setIsFullscreen(false);
        }
    };


    // Get group names for display
    const groupNames = groups.map(g => g.name);

    // Helper to get icon for a page
    const getPageIcon = (pageId: string) => {
        const base = pageId.split('_')[0].toLowerCase();
        if (base === 'training') return <Target className="h-3 w-3 mr-2" />;
        if (base === 'inventory' || base === 'items') return <Package className="h-3 w-3 mr-2" />;
        if (base === 'skills') return <Zap className="h-3 w-3 mr-2" />;
        if (base === 'environment') return <Globe className="h-3 w-3 mr-2" />;
        if (base === 'log') return <FileText className="h-3 w-3 mr-2" />;
        if (base === 'packetsniffer' || base === 'activity') return <Activity className="h-3 w-3 mr-2" />;
        return <div className="w-1.5 h-1.5 rounded-full bg-slate-300 dark:bg-slate-700 mr-2" />;
    };

    return (
        <div className="flex h-screen bg-background text-foreground font-sans transition-colors duration-300">
            {/* Sidebar */}
            <div className="w-64 border-r bg-card flex flex-col transition-colors duration-300">
                <div className="p-4 border-b flex items-center justify-between">
                    <div className="font-bold text-lg text-primary font-mono flex items-center gap-2">
                        <Monitor className="h-5 w-5" />
                        Sokybot v2
                    </div>
                </div>

                {/* Actions */}
                <div className="p-2 grid grid-cols-2 gap-2 border-b">
                    <Button variant="outline" size="sm" onClick={() => setCreateGroupOpen(true)} className="h-8">
                        <Plus className="h-3 w-3 mr-1" /> Group
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => setCreateMachineOpen(true)} className="h-8">
                        <Plus className="h-3 w-3 mr-1" /> Bot
                    </Button>
                </div>

                <div className="flex-1 overflow-y-auto p-2 space-y-4">
                    {groupNames.map(group => {
                        const groupMachines = machines.filter(m =>
                            m.groupName === group || getMachineGroup(m.machineId) === group
                        );
                        return (
                            <div key={group} className="space-y-1">
                                <div className="px-2 text-xs font-semibold text-muted-foreground uppercase tracking-wider flex items-center justify-between">
                                    {group}
                                    <span className="text-[10px] bg-secondary px-1 rounded text-foreground">{groupMachines.length}</span>
                                </div>
                                {groupMachines.map(m => (
                                    /* Machine Item */
                                    <div key={m.machineId} className="space-y-1">
                                        <Button
                                            key={m.machineId}
                                            variant={selectedMachineId === m.machineId ? "secondary" : "ghost"}
                                            className={cn("w-full justify-start font-normal pl-4 h-8", selectedMachineId === m.machineId && "font-medium bg-accent")}
                                            onClick={() => {
                                                setSelectedMachineId(m.machineId);
                                                // Default to 'training' or first tab if not set
                                                // Logic will be handled by MachineView or here
                                                // For now just select machine, let MachineView select default tab
                                            }}
                                        >
                                            <Bot className={cn("h-4 w-4 mr-2", m.isRunning ? "text-green-500" : "text-muted-foreground")} />
                                            {m.name || getSimpleName(m.machineId)}
                                        </Button>

                                        {/* Nested Pages (Tree View) */}
                                        {selectedMachineId === m.machineId && (
                                            <div className="ml-4 pl-2 border-l border-slate-200 dark:border-slate-800 space-y-0.5 min-h-[4px]">
                                                {getMachineTabs(m.machineId).length > 0 ? (
                                                    getMachineTabs(m.machineId).map(tabId => {
                                                        const page = extensionRegistry.pages[tabId];
                                                        const isActive = selectedPageId === tabId;
                                                        return (
                                                            <Button
                                                                key={tabId}
                                                                variant="ghost"
                                                                size="sm"
                                                                className={cn(
                                                                    "w-full justify-start h-7 text-xs font-normal",
                                                                    isActive ? "text-primary bg-primary/10" : "text-muted-foreground"
                                                                )}
                                                                onClick={() => setSelectedPageId(tabId)}
                                                            >
                                                                <div className="ml-1 flex items-center">
                                                                    {getPageIcon(tabId)}
                                                                    {page?.title || tabId}
                                                                </div>
                                                            </Button>
                                                        );
                                                    })
                                                ) : (
                                                    <div className="text-[10px] text-muted-foreground italic pl-3 py-1">
                                                        No pages registered
                                                    </div>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                ))}
                            </div>
                        );
                    })}

                    {/* Orphaned machines or if no groups defined yet (legacy fallback) */}
                    {groupNames.length === 0 && machines.length > 0 && (
                        <div className="space-y-1">
                            <div className="px-2 text-xs font-semibold text-muted-foreground uppercase tracking-wider">Default</div>
                            {machines.map(m => (
                                <div key={m.machineId} className="space-y-1">
                                    <Button
                                        variant={selectedMachineId === m.machineId ? "secondary" : "ghost"}
                                        className={cn("w-full justify-start font-normal", selectedMachineId === m.machineId && "font-medium")}
                                        onClick={() => setSelectedMachineId(m.machineId)}
                                    >
                                        <Bot className={cn("h-4 w-4 mr-2", m.isRunning ? "text-green-500" : "text-muted-foreground")} />
                                        {m.name || m.machineId}
                                    </Button>

                                    {/* Nested Pages (Tree View) */}
                                    {selectedMachineId === m.machineId && (
                                        <div className="ml-4 pl-2 border-l border-slate-200 dark:border-slate-800 space-y-0.5 min-h-[4px]">
                                            {getMachineTabs(m.machineId).length > 0 ? (
                                                getMachineTabs(m.machineId).map(tabId => {
                                                    const page = extensionRegistry.pages[tabId];
                                                    const isActive = selectedPageId === tabId;
                                                    return (
                                                        <Button
                                                            key={tabId}
                                                            variant="ghost"
                                                            size="sm"
                                                            className={cn(
                                                                "w-full justify-start h-7 text-xs font-normal",
                                                                isActive ? "text-primary bg-primary/10" : "text-muted-foreground"
                                                            )}
                                                            onClick={() => setSelectedPageId(tabId)}
                                                        >
                                                            <div className="ml-1 flex items-center">
                                                                {getPageIcon(tabId)}
                                                                {page?.title || tabId}
                                                            </div>
                                                        </Button>
                                                    );
                                                })
                                            ) : (
                                                <div className="text-[10px] text-muted-foreground italic pl-3 py-1">
                                                    No pages registered
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}

                    {groupNames.length === 0 && machines.length === 0 && (
                        <div className="text-muted-foreground text-xs italic p-4 text-center">
                            No groups or bots found.<br />Create a group to start.
                        </div>
                    )}
                </div>
                <div className="p-4 border-t text-xs text-muted-foreground">
                    {machines.length} Total Bots
                </div>
            </div>

            <CreateGroupDialog
                isOpen={isCreateGroupOpen}
                onClose={() => setCreateGroupOpen(false)}
                onCreated={fetchInitialData}
            />
            <CreateMachineDialog
                isOpen={isCreateMachineOpen}
                onClose={() => setCreateMachineOpen(false)}
                onSuccess={fetchInitialData}
            />

            {/* Main Content Area */}
            <div className="flex-1 flex flex-col min-w-0 bg-secondary/10 overflow-hidden">
                {/* Header */}
                <div className="bg-card border-b p-4 shadow-sm flex justify-between items-center h-16 transition-colors duration-300">
                    <div className="font-semibold text-lg flex items-center gap-2">
                        {selectedMachineId ? (
                            <>
                                <Bot className="h-5 w-5 text-primary" />
                                <span className="text-muted-foreground mr-1">Machine:</span>
                                <span>{getSimpleName(selectedMachineId)}</span>
                            </>
                        ) : "Select a Machine"}
                    </div>
                    <div className="flex items-center gap-2">
                        {/* Extension Toolbar Actions */}
                        <ToolbarExtensions />

                        <Button variant="ghost" size="icon" onClick={() => {
                            if (theme === 'light') setTheme('dark');
                            else if (theme === 'dark') setTheme('desktop');
                            else if (theme === 'desktop') setTheme('desktop-dark');
                            else setTheme('light');
                        }} title={`Theme: ${theme}`}>
                            {theme === 'dark' ? <Moon className="h-4 w-4" /> :
                                theme === 'desktop' ? <Monitor className="h-4 w-4" /> :
                                    theme === 'desktop-dark' ? <Monitor className="h-4 w-4 text-foreground/70" /> :
                                        <Sun className="h-4 w-4" />}
                        </Button>

                        <Button variant="ghost" size="icon" onClick={toggleFullscreen}>
                            {isFullscreen ? <Minimize className="h-4 w-4" /> : <Maximize className="h-4 w-4" />}
                        </Button>
                        <div className="h-4 w-px bg-border mx-1" />
                        <Button variant="ghost" size="icon">
                            <Settings className="h-4 w-4" />
                        </Button>
                    </div>
                </div>

                <div className="flex-1 overflow-auto p-6 transition-colors duration-300">
                    <div className="max-w-6xl mx-auto h-full">
                        {children}
                    </div>
                </div>
            </div>

            {/* Right Sidebar (Dashboard) */}
            {selectedMachineId && (
                <div className="w-72 border-l bg-card flex flex-col p-4 transition-colors duration-300 overflow-y-auto shadow-[-4px_0_10px_rgba(0,0,0,0.02)]">
                    <div className="flex items-center gap-2 mb-4 text-[10px] font-bold text-muted-foreground uppercase tracking-widest border-b border-border/50 pb-2">
                        <Monitor className="h-3 w-3" />
                        Bot Dashboard
                    </div>
                    <CharacterStatus machineId={selectedMachineId} />
                </div>
            )}
        </div>
    );
};
