import React, { useState } from 'react';
import { useSokybotStore } from './store';
import { useExtensionRegistry } from './extensions/ExtensionRegistry';
import { useTheme } from './components/theme-provider';
import { CreateGroupDialog } from './components/CreateGroupDialog';
import CreateMachineDialog from './components/CreateMachineDialog';
import { ToolbarExtensions } from './components/ToolbarExtensions';
import { MachineOnboardingSection } from './components/MachineOnboardingSection';
import { DiagnosticsPanel } from './components/DiagnosticsPanel';
import { useMachinesQuery, useGroupsQuery, useInvalidateSokybotQueries } from './query/sokybotQueries';
import { useMachineSeverityStore } from './machines/useMachineSeverityStore';
import { Plus, Bot, Monitor, Moon, Sun, Maximize, Minimize, Settings, Target, Package, Zap, Globe, FileText, Activity } from 'lucide-react';
import { Button } from '@sokybot/frontend-shared';
import { cn } from '@sokybot/frontend-shared';

interface LayoutProps {
    children: React.ReactNode;
    isBackendConnected: boolean;
}

/** Inline severity indicator for machine list items in the sidebar. */
const SEVERITY_DOT_CLASSES: Record<string, string> = {
    error: 'bg-red-500',
    warn: 'bg-amber-500',
    info: 'bg-blue-400',
    success: 'bg-emerald-500',
};

const SeverityDot: React.FC<{ machineId: string }> = ({ machineId }) => {
    const severity = useMachineSeverityStore((s) => s.severityByMachine[machineId]);
    if (!severity || severity === 'neutral') return null;
    const dotClass = SEVERITY_DOT_CLASSES[severity] ?? '';
    if (!dotClass) return null;
    return (
        <span
            className={cn('ml-auto h-2 w-2 rounded-full flex-shrink-0', dotClass)}
            title={`Severity: ${severity}`}
        />
    );
};

interface MachineListItemProps {
    machine: { machineId: string; name?: string; isRunning?: boolean };
    selectedMachineId: string | null;
    selectedPageId: string | null;
    setSelectedMachineId: (id: string | null) => void;
    setSelectedPageId: (id: string | null) => void;
    getMachineTabs: (machineId: string) => string[];
    getPageIcon: (pageId: string) => React.ReactNode;
    getSimpleName: (machineId: string) => string;
    extensionPages: Record<string, { title?: string }>;
    selectedIndicatorClass: string;
    nestedTreeClass: string;
}

const MachineListItem: React.FC<MachineListItemProps> = ({
    machine,
    selectedMachineId,
    selectedPageId,
    setSelectedMachineId,
    setSelectedPageId,
    getMachineTabs,
    getPageIcon,
    getSimpleName,
    extensionPages,
    selectedIndicatorClass,
    nestedTreeClass,
}) => {
    const tabs = getMachineTabs(machine.machineId);
    const isSelected = selectedMachineId === machine.machineId;

    return (
        <div key={machine.machineId} className="space-y-1">
            <Button
                variant={isSelected ? "secondary" : "ghost"}
                className={cn(
                    "w-full justify-start font-normal pl-3 h-9 transition-all duration-200 group relative overflow-hidden",
                    isSelected
                        ? "font-medium bg-primary/10 text-primary hover:bg-primary/15"
                        : "text-muted-foreground hover:text-foreground hover:bg-secondary/40"
                )}
                onClick={() => {
                    setSelectedMachineId(machine.machineId);
                }}
            >
                <Bot className={cn("h-4 w-4 mr-2.5 transition-colors",
                    machine.isRunning ? "text-emerald-500" : "text-muted-foreground/85 group-hover:text-foreground"
                )} />
                <span className="truncate">{machine.name || getSimpleName(machine.machineId)}</span>
                <SeverityDot machineId={machine.machineId} />
                {isSelected && (
                    <div className={selectedIndicatorClass} />
                )}
            </Button>

            {isSelected && (
                <div className={nestedTreeClass}>
                    {tabs.length > 0 ? (
                        tabs.map(tabId => {
                            const page = extensionPages[tabId];
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
                                            : "text-muted-foreground/90 hover:text-foreground hover:bg-secondary/30"
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
                        <div className="text-[10px] text-muted-foreground/75 italic pl-2 py-1.5">
                            No pages registered
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export const Layout: React.FC<LayoutProps> = ({ children, isBackendConnected }) => {
    const {
        selectedMachineId,
        setSelectedMachineId,
    } = useSokybotStore();

    const { data: machines = [] } = useMachinesQuery(isBackendConnected);
    const { data: groups = [] } = useGroupsQuery(isBackendConnected);
    const { invalidateMachines, invalidateGroups } = useInvalidateSokybotQueries();

    const refreshMachineLists = () => {
        void invalidateMachines();
        void invalidateGroups();
    };

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

        Object.keys(extensionRegistry.pages || {}).forEach(pageId => {
            // Packet Analyzer is shown only as a modal from Packet Sniffer, not as a separate tab
            if (pageId.toLowerCase().startsWith('packetanalyzer_')) return;

            // Strict match: pageId must end with exactly _machineId
            if (pageId === machineId ||
                pageId.endsWith("_" + machineId)) {
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
    const [stableInGameByMachine, setStableInGameByMachine] = useState<Record<string, boolean>>({});

    const toggleFullscreen = () => {
        if (!document.fullscreenElement) {
            document.documentElement.requestFullscreen();
            setIsFullscreen(true);
        } else {
            document.exitFullscreen();
            setIsFullscreen(false);
        }
    };


    // Normalize machines once so rendering stays stable even if backend sends duplicate rows.
    const uniqueMachines = React.useMemo(() => {
        const byId = new Map<string, typeof machines[number]>();
        machines.forEach((machine) => {
            if (!machine?.machineId) return;
            const existing = byId.get(machine.machineId);
            byId.set(machine.machineId, {
                ...(existing || {}),
                ...machine,
                groupName: machine.groupName || existing?.groupName
            });
        });
        return Array.from(byId.values());
    }, [machines]);

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
        return <div className="w-1.5 h-1.5 rounded-full bg-muted-foreground/50 mr-2" />;
    };

    const hasSystemLog = !!extensionRegistry.pages?.['SystemLog'];

    return (
        <div className="flex h-screen bg-background text-foreground font-sans transition-colors duration-300 overflow-hidden">
            {/* Sidebar */}
            <div className="w-64 min-h-0 border-r border-border bg-card/80 backdrop-blur-md flex flex-col transition-all duration-300 z-20">
                <div className="h-16 px-6 border-b border-border flex items-center justify-between bg-card/50">
                    <div className="font-bold text-lg text-primary font-mono flex items-center gap-3 tracking-tight">
                        <div className="p-1.5 rounded-md bg-primary/10">
                            <Monitor className="h-5 w-5" />
                        </div>
                        Sokybot v2
                    </div>
                </div>

                {/* Actions */}
                <div className="p-4 grid grid-cols-2 gap-3 border-b border-border">
                    <Button variant="outline" size="sm" onClick={() => setCreateGroupOpen(true)} className="h-9 shadow-sm hover:shadow transition-all hover:bg-primary/5 hover:text-primary hover:border-primary/20">
                        <Plus className="h-3.5 w-3.5 mr-1.5" /> Group
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => setCreateMachineOpen(true)} className="h-9 shadow-sm hover:shadow transition-all hover:bg-primary/5 hover:text-primary hover:border-primary/20">
                        <Plus className="h-3.5 w-3.5 mr-1.5" /> Bot
                    </Button>
                </div>

                <div className="flex-1 overflow-y-auto p-3 space-y-6 scrollbar-thin scrollbar-thumb-border scrollbar-track-transparent">
                    {groupNames.map(group => {
                        const groupMachines = uniqueMachines.filter(m =>
                            (m.groupName || getMachineGroup(m.machineId)) === group
                        );
                        const groupLogPageId = `GroupLog_${group}`;
                        const hasGroupLog = !!extensionRegistry.pages?.[groupLogPageId];
                        return (
                            <div key={group} className="space-y-1">
                                <div className="px-3 text-[10px] font-bold text-muted-foreground/90 uppercase tracking-widest flex items-center justify-between mb-2">
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
                                        <span className="text-[9px] bg-secondary px-1.5 py-0.5 rounded-full text-secondary-foreground font-mono shadow-sm border border-border/60">{groupMachines.length}</span>
                                    </div>
                                </div>
                                {groupMachines.map((m) => (
                                    <MachineListItem
                                        key={m.machineId}
                                        machine={m}
                                        selectedMachineId={selectedMachineId}
                                        selectedPageId={selectedPageId}
                                        setSelectedMachineId={setSelectedMachineId}
                                        setSelectedPageId={setSelectedPageId}
                                        getMachineTabs={getMachineTabs}
                                        getPageIcon={getPageIcon}
                                        getSimpleName={getSimpleName}
                                        extensionPages={extensionRegistry.pages}
                                        selectedIndicatorClass="absolute left-0 top-0 bottom-0 w-0.5 bg-primary animate-in fade-in slide-in-from-left-1 duration-300"
                                        nestedTreeClass="ml-5 pl-3 border-l border-border/30 space-y-0.5 my-1 animate-in slide-in-from-top-2 duration-200 fade-in-0"
                                    />
                                ))}
                            </div>
                        );
                    })}

                    {/* Orphaned machines or if no groups defined yet (legacy fallback) */}
                    {groupNames.length === 0 && uniqueMachines.length > 0 && (
                        <div className="space-y-1">
                            <div className="px-3 text-[10px] font-bold text-muted-foreground/90 uppercase tracking-widest mb-2">Default</div>
                            {uniqueMachines.map((m) => (
                                <MachineListItem
                                    key={m.machineId}
                                    machine={m}
                                    selectedMachineId={selectedMachineId}
                                    selectedPageId={selectedPageId}
                                    setSelectedMachineId={setSelectedMachineId}
                                    setSelectedPageId={setSelectedPageId}
                                    getMachineTabs={getMachineTabs}
                                    getPageIcon={getPageIcon}
                                    getSimpleName={getSimpleName}
                                    extensionPages={extensionRegistry.pages}
                                    selectedIndicatorClass="absolute left-0 top-0 bottom-0 w-0.5 bg-primary rounded-r-full"
                                    nestedTreeClass="ml-5 pl-3 border-l border-border/70 space-y-0.5 my-1 animate-in slide-in-from-top-2 duration-200 fade-in-0"
                                />
                            ))}
                        </div>
                    )}

                    {groupNames.length === 0 && uniqueMachines.length === 0 && (
                        <div className="text-muted-foreground text-xs italic p-8 text-center border-2 border-dashed border-border rounded-lg mx-2 bg-secondary/30">
                            No groups or bots found.<br />Create a group to start.
                        </div>
                    )}
                </div>
                <DiagnosticsPanel isBackendConnected={isBackendConnected} />
                <div className="p-4 border-t border-border text-[10px] font-medium text-muted-foreground flex justify-between items-center bg-muted/50">
                    <span>{uniqueMachines.length} Total Bots</span>
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
                onCreated={refreshMachineLists}
            />
            <CreateMachineDialog
                isOpen={isCreateMachineOpen}
                onClose={() => setCreateMachineOpen(false)}
                onSuccess={refreshMachineLists}
            />

            {/* Main Content Area */}
            <div className="flex-1 flex flex-col min-w-0 min-h-0 bg-background relative">
                <div className="absolute inset-0 bg-gradient-to-tr from-primary/[0.06] via-transparent to-transparent pointer-events-none opacity-40" />

                {/* Header */}
                <div className="h-16 px-6 border-b border-border bg-card shadow-sm flex justify-between items-center z-10 transition-colors duration-300">
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

                <div className="flex-1 min-h-0 overflow-hidden transition-colors duration-300 z-0 relative flex flex-col">
                    <div className="flex-1 min-h-0 flex flex-col overflow-hidden h-full">
                        {children}
                    </div>
                </div>
            </div>

            {/* Right Sidebar (Dashboard) */}
            {selectedMachineId && (
                <MachineOnboardingSection
                    key={selectedMachineId}
                    machineId={selectedMachineId}
                    compact={Boolean(stableInGameByMachine[selectedMachineId])}
                    onStableInGameChange={(machineId, stableInGame) => {
                        setStableInGameByMachine((prev) => {
                            if (prev[machineId] === stableInGame) return prev;
                            return { ...prev, [machineId]: stableInGame };
                        });
                    }}
                />
            )}
        </div>
    );
};
