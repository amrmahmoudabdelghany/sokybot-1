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
            // Packet Analyzer is shown only as a modal from Packet Sniffer, not as a separate tab
            if (lowerPageId.startsWith('packetanalyzer_')) return;

            // Strict match: pageId must end with exactly _machineId
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

    const hasSystemLog = !!extensionRegistry.pages?.['SystemLog'];

    return (
        <div className="flex h-screen bg-background text-foreground font-sans transition-colors duration-300 overflow-hidden">
            {/* Sidebar */}
            <div className="w-64 border-r border-border/40 bg-card/80 backdrop-blur-md flex flex-col transition-all duration-300 z-20">
                <div className="h-16 px-6 border-b border-border/40 flex items-center justify-between bg-card/50">
                    <div className="font-bold text-lg text-primary font-mono flex items-center gap-3 tracking-tight">
                        <div className="p-1.5 rounded-md bg-primary/10">
                            <Monitor className="h-5 w-5" />
                        </div>
                        Sokybot v2
                    </div>
                </div>

                {/* Actions */}
                <div className="p-4 grid grid-cols-2 gap-3 border-b border-border/40">
                    <Button variant="outline" size="sm" onClick={() => setCreateGroupOpen(true)} className="h-9 shadow-sm hover:shadow transition-all hover:bg-primary/5 hover:text-primary hover:border-primary/20">
                        <Plus className="h-3.5 w-3.5 mr-1.5" /> Group
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => setCreateMachineOpen(true)} className="h-9 shadow-sm hover:shadow transition-all hover:bg-primary/5 hover:text-primary hover:border-primary/20">
                        <Plus className="h-3.5 w-3.5 mr-1.5" /> Bot
                    </Button>
                </div>

                <div className="flex-1 overflow-y-auto p-3 space-y-6 scrollbar-thin scrollbar-thumb-border/40 scrollbar-track-transparent">
                    {groupNames.map(group => {
                        const groupMachines = machines.filter(m =>
                            m.groupName === group || getMachineGroup(m.machineId) === group
                        );
                        const groupLogPageId = `GroupLog_${group}`;
                        const hasGroupLog = !!extensionRegistry.pages?.[groupLogPageId];
                        return (
                            <div key={group} className="space-y-1">
                                <div className="px-3 text-[10px] font-bold text-muted-foreground/70 uppercase tracking-widest flex items-center justify-between mb-2">
                                    <span>{group}</span>
                                    <div className="flex items-center gap-1">
                                        {hasGroupLog && (
                                            <Button
                                                variant="ghost"
                                                size="icon"
                                                className="h-6 w-6 text-muted-foreground hover:text-primary"
                                                title="Group Log"
                                                onClick={() => {
                                                    setSelectedMachineId(null);
                                                    setSelectedPageId(groupLogPageId);
                                                }}
                                            >
                                                <FileText className="h-3 w-3" />
                                            </Button>
                                        )}
                                        <span className="text-[9px] bg-secondary/80 px-1.5 py-0.5 rounded-full text-foreground/80 font-mono shadow-sm">{groupMachines.length}</span>
                                    </div>
                                </div>
                                {groupMachines.map(m => (
                                    /* Machine Item */
                                    <div key={m.machineId} className="space-y-1">
                                        <Button
                                            key={m.machineId}
                                            variant={selectedMachineId === m.machineId ? "secondary" : "ghost"}
                                            className={cn(
                                                "w-full justify-start font-normal pl-3 h-9 transition-all duration-200 group relative overflow-hidden",
                                                selectedMachineId === m.machineId
                                                    ? "font-medium bg-primary/10 text-primary hover:bg-primary/15"
                                                    : "text-muted-foreground hover:text-foreground hover:bg-secondary/40"
                                            )}
                                            onClick={() => {
                                                setSelectedMachineId(m.machineId);
                                            }}
                                        >
                                            <Bot className={cn("h-4 w-4 mr-2.5 transition-colors",
                                                m.isRunning ? "text-emerald-500" : "text-muted-foreground/60 group-hover:text-muted-foreground"
                                            )} />
                                            <span className="truncate">{m.name || getSimpleName(m.machineId)}</span>
                                            {selectedMachineId === m.machineId && (
                                                <div className="absolute left-0 top-0 bottom-0 w-0.5 bg-primary animate-in fade-in slide-in-from-left-1 duration-300" />
                                            )}
                                        </Button>

                                        {/* Nested Pages (Tree View) */}
                                        {selectedMachineId === m.machineId && (
                                            <div className="ml-5 pl-3 border-l border-border/30 space-y-0.5 my-1 animate-in slide-in-from-top-2 duration-200 fade-in-0">
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
                                                                    "w-full justify-start h-8 text-xs font-normal transition-colors",
                                                                    isActive
                                                                        ? "text-primary bg-primary/5 font-medium"
                                                                        : "text-muted-foreground/80 hover:text-foreground hover:bg-secondary/30"
                                                                )}
                                                                onClick={() => setSelectedPageId(tabId)}
                                                            >
                                                                <div className="flex items-center">
                                                                    {getPageIcon(tabId)}
                                                                    {page?.title || tabId}
                                                                </div>
                                                            </Button>
                                                        );
                                                    })
                                                ) : (
                                                    <div className="text-[10px] text-muted-foreground/50 italic pl-2 py-1.5">
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
                            <div className="px-3 text-[10px] font-bold text-muted-foreground/70 uppercase tracking-widest mb-2">Default</div>
                            {machines.map(m => (
                                <div key={m.machineId} className="space-y-1">
                                    <Button
                                        variant={selectedMachineId === m.machineId ? "secondary" : "ghost"}
                                        className={cn(
                                            "w-full justify-start font-normal pl-3 h-9 transition-all duration-200 group relative",
                                            selectedMachineId === m.machineId
                                                ? "font-medium bg-primary/10 text-primary hover:bg-primary/15"
                                                : "text-muted-foreground hover:text-foreground hover:bg-secondary/40"
                                        )}
                                        onClick={() => setSelectedMachineId(m.machineId)}
                                    >
                                        <Bot className={cn("h-4 w-4 mr-2.5 transition-colors",
                                            m.isRunning ? "text-emerald-500" : "text-muted-foreground/60 group-hover:text-muted-foreground"
                                        )} />
                                        <span className="truncate">{m.name || m.machineId}</span>
                                        {selectedMachineId === m.machineId && (
                                            <div className="absolute left-0 top-0 bottom-0 w-0.5 bg-primary rounded-r-full" />
                                        )}
                                    </Button>

                                    {/* Nested Pages (Tree View) */}
                                    {selectedMachineId === m.machineId && (
                                        <div className="ml-5 pl-3 border-l border-border/30 space-y-0.5 my-1 animate-in slide-in-from-top-2 duration-200 fade-in-0">
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
                                                                "w-full justify-start h-8 text-xs font-normal transition-colors",
                                                                isActive
                                                                    ? "text-primary bg-primary/5 font-medium"
                                                                    : "text-muted-foreground/80 hover:text-foreground hover:bg-secondary/30"
                                                            )}
                                                            onClick={() => setSelectedPageId(tabId)}
                                                        >
                                                            <div className="flex items-center">
                                                                {getPageIcon(tabId)}
                                                                {page?.title || tabId}
                                                            </div>
                                                        </Button>
                                                    );
                                                })
                                            ) : (
                                                <div className="text-[10px] text-muted-foreground/50 italic pl-2 py-1.5">
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
                        <div className="text-muted-foreground text-xs italic p-8 text-center border-2 border-dashed border-border/30 rounded-lg mx-2 bg-secondary/10">
                            No groups or bots found.<br />Create a group to start.
                        </div>
                    )}
                </div>
                <div className="p-4 border-t border-border/40 text-[10px] font-medium text-muted-foreground/60 flex justify-between items-center bg-card/30">
                    <span>{machines.length} Total Bots</span>
                    <div className="flex items-center gap-2">
                        {hasSystemLog && (
                            <Button
                                variant="ghost"
                                size="sm"
                                className="h-6 px-2 text-[10px] font-medium text-muted-foreground hover:text-primary hover:bg-secondary/40"
                                onClick={() => {
                                    setSelectedMachineId(null);
                                    setSelectedPageId('SystemLog');
                                }}
                            >
                                <FileText className="h-3 w-3 mr-1" />
                                System Log
                            </Button>
                        )}
                        <span className="w-2 h-2 rounded-full bg-emerald-500/20 ring-1 ring-emerald-500/40"></span>
                    </div>
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
            <div className="flex-1 flex flex-col min-w-0 bg-background/50 relative">
                {/* Decorative background gradients */}
                <div className="absolute inset-0 bg-gradient-to-tr from-primary/5 via-transparent to-transparent pointer-events-none opacity-50" />

                {/* Header */}
                <div className="h-16 px-6 border-b border-border/40 bg-card/80 backdrop-blur-md shadow-sm flex justify-between items-center z-10 transition-colors duration-300">
                    <div className="font-semibold text-lg flex items-center gap-3">
                        {selectedMachineId ? (
                            <div className="flex items-center animate-in fade-in slide-in-from-left-2 duration-300">
                                <div className="p-1.5 bg-primary/10 rounded-md mr-3 text-primary">
                                    <Bot className="h-5 w-5" />
                                </div>
                                <div className="flex flex-col leading-none">
                                    <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider mb-0.5">Active Machine</span>
                                    <span className="text-base text-foreground font-medium">{getSimpleName(selectedMachineId)}</span>
                                </div>
                            </div>
                        ) : (
                            <span className="text-muted-foreground flex items-center gap-2">
                                <Target className="h-4 w-4" /> Select a Machine
                            </span>
                        )}
                    </div>
                    <div className="flex items-center gap-1.5">
                        {/* Extension Toolbar Actions */}
                        <div className="mr-2">
                            <ToolbarExtensions />
                        </div>

                        <div className="h-6 w-px bg-border/40 mx-2" />

                        <Button variant="ghost" size="icon" className="h-9 w-9 text-muted-foreground hover:text-foreground" onClick={() => {
                            if (theme === 'light') setTheme('dark');
                            else if (theme === 'dark') setTheme('desktop');
                            else if (theme === 'desktop') setTheme('desktop-dark');
                            else setTheme('light');
                        }} title={`Theme: ${theme}`}>
                            {theme === 'dark' ? <Moon className="h-4 w-4 transition-all" /> :
                                theme === 'desktop' ? <Monitor className="h-4 w-4 transition-all" /> :
                                    theme === 'desktop-dark' ? <Monitor className="h-4 w-4 text-primary transition-all" /> :
                                        <Sun className="h-4 w-4 transition-all" />}
                        </Button>

                        <Button variant="ghost" size="icon" className="h-9 w-9 text-muted-foreground hover:text-foreground" onClick={toggleFullscreen}>
                            {isFullscreen ? <Minimize className="h-4 w-4" /> : <Maximize className="h-4 w-4" />}
                        </Button>
                        <Button variant="ghost" size="icon" className="h-9 w-9 text-muted-foreground hover:text-foreground">
                            <Settings className="h-4 w-4" />
                        </Button>
                    </div>
                </div>

                <div className="flex-1 overflow-auto transition-colors duration-300 z-0 relative">
                    <div className="h-full">
                        {children}
                    </div>
                </div>
            </div>

            {/* Right Sidebar (Dashboard) */}
            {selectedMachineId && (
                <div className="w-80 border-l border-border/40 bg-card/80 backdrop-blur-md flex flex-col transition-all duration-300 z-20 shadow-[-4px_0_20px_rgba(0,0,0,0.02)]">
                    <div className="h-16 px-4 flex items-center border-b border-border/40">
                        <div className="flex items-center gap-2 text-[11px] font-bold text-muted-foreground uppercase tracking-widest">
                            <Activity className="h-4 w-4 text-primary" />
                            Live Dashboard
                        </div>
                    </div>
                    <div className="flex-1 overflow-y-auto p-4">
                        <CharacterStatus machineId={selectedMachineId} />
                    </div>
                </div>
            )}
        </div>
    );
};
